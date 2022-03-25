#include <jvmti.h>
#include <pthread.h>
#include <time.h>
#include <sys/time.h>
#include <stdlib.h>
#include <stdint.h>
#include <unistd.h>
#include <string.h>
#include <stdbool.h>
#ifdef __MVS__
#else
#include <semaphore.h>
#endif
#include <pthread.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <errno.h>
#include <sys/poll.h>
#include "agent.h"

JNIEnv *localJNIInterface;

JNIEnv* globalJNI;
JavaVM *globalJVM;
jvmtiEnv *globalJVMTIInterface;
pthread_t stackSamplingThreadHandle, networkThreadHandle;
jvmtiExtensionFunction getOSThreadID;
MethodIDToNameHashTable *methodIDToNameHashTable = NULL;
FIFO *fifo = NULL;
pthread_cond_t moreDataCond;
pthread_cond_t networkShutdownCond;

pthread_mutex_t moreDataMutex;
pthread_mutex_t networkShutdownMutex;

uint64_t hertz = DEFAULT_HERTZ;

volatile uint32_t clientState = DISCONNECTED;
//uint32_t state = RUNNING;

volatile bool alive;

void
createMethodIDToNameHashTable(void)
{

  methodIDToNameHashTable = calloc(1, sizeof(MethodIDToNameHashTable));
  if (!methodIDToNameHashTable)
    {
      fprintf(stderr, "Unable to allocate memory\n");
      exit(-1);
    }
}

void
clearMethodIDToNameHashTable()
{

  int i = 0;

  MethodIDToNameBucket *bucket;

  for (i = 0; i < METHOD_ID_TO_NAME_HASHTABLE_SIZE; i++)
    {

      bucket = methodIDToNameHashTable->buckets[i];
      if (bucket)
        {
          MethodIDToNameNode *node = bucket->rootNode;

          while (node)
            {
              MethodIDToNameNode *nextNode = node->next;
              if (node->classSignature)
                free(node->classSignature);
              if (node->methodName)
                free(node->methodName);
              if (node->methodSignature)
                free(node->methodSignature);
              free(node);
              node = nextNode;

            }
          bucket->rootNode = NULL; // PA Sunday 24/2

        }

    }

  methodIDToNameHashTable->entries = 0;
  methodIDToNameHashTable->collisions = 0;

}

void
deleteMethodIDToJinsightClassMethodIDHashTable()
{

  if (methodIDToNameHashTable)
    {

      int i;

      MethodIDToNameBucket *bucket;

      for (i = 0; i < METHOD_ID_TO_NAME_HASHTABLE_SIZE; i++)
        {

          bucket = methodIDToNameHashTable->buckets[i];

          if (bucket != NULL)
            {
              MethodIDToNameNode *node = bucket->rootNode;

              while (node)
                {
                  MethodIDToNameNode *nextNode = node->next;
                  if (node->classSignature)
                    free(node->classSignature);
                  if (node->methodName)
                    free(node->methodName);
                  if (node->methodSignature)
                    free(node->methodSignature);
                  free(node);
                  node = nextNode;

                }
              free(bucket);

            }

        }

      free(methodIDToNameHashTable);
    }
}

void
addToMethodIDToNameHashTable(jmethodID methodID, char* classSignature, char* methodName, char* methodSignature)
{

  uint32_t bucketNumber = hashMethodID(methodID);

  //fetchAndIncrement(&methodIDToNameHashTable->entries);

  MethodIDToNameNode *theNode = malloc(sizeof(MethodIDToNameNode));
  if (!theNode)
    {
      fprintf(stderr, "Unable to allocate memory\n");
      exit(-1);
    }

  theNode->methodID = methodID;
  theNode->classSignature = strdup(classSignature);
  theNode->methodName = strdup(methodName);
  theNode->methodSignature = strdup(methodSignature);
  theNode->next = NULL;

  lock((&methodIDToNameHashTable->lock));

  MethodIDToNameBucket *bucket = methodIDToNameHashTable->buckets[bucketNumber];

  if (bucket == NULL)
    {

      bucket = malloc(sizeof(MethodIDToNameBucket));
      if (!bucket)
        {
          fprintf(stderr, "Unable to allocate memory\n");
          exit(-1);
        }

      bucket->rootNode = NULL;
      bucket->lock = UNLOCKED;
      methodIDToNameHashTable->buckets[bucketNumber] = bucket;
    }

  lock((&bucket->lock));
  unlock((&methodIDToNameHashTable->lock));

  MethodIDToNameNode *node = bucket->rootNode;

  if (node == NULL)
    {

      bucket->rootNode = theNode;

    }
  else
    {

      //   fetchAndIncrement(&methodIDToNameHashTable->collisions);

      while (node->next != NULL)
        {

          node = node->next;
        }

      node->next = theNode;
    }

  unlock((&bucket->lock));
}

MethodIDToNameNode*
getMethodIDToJinsightClassMethodID(jmethodID methodID)
{

  uint32_t bucketNumber = hashMethodID(methodID);

  MethodIDToNameBucket *bucket = methodIDToNameHashTable->buckets[bucketNumber];

  if (bucket == NULL)
    {
      return NULL;
    }

  lock(&(bucket->lock));

  MethodIDToNameNode *node = bucket->rootNode;

  if (node == NULL)
    {
      unlock(&(bucket->lock));
      return NULL;
    }

  while ((node != NULL) && (node->methodID != methodID))
    {
      node = node->next;
    }

  unlock(&(bucket->lock));

  return node;

}

#ifdef __MVS__

char *E2A;
char *A2E;

void createLUTs()
  {

    E2A = (char*) calloc(1, 256);
    A2E = (char*) calloc(1, 256);

    int i;
    int ret = 0;
    for(i=1;i<256;i++)
      {
        E2A[i] = i;
      }

    ret = __e2a_l(E2A+1, 254);

    for(i=1;i<256;i++)
      {
        A2E[i] = i;
      }

    ret = __a2e_l(A2E+1, 254);

  }
#endif

uint32_t
compareJVMString(const char *string, char *jvmString)
{

  if ((string == NULL) || (jvmString == NULL))
    return 0;

  int stringLength = 0;

  char *pointer = (char*) string;
  while (*pointer != 0)
    {
      pointer++;
      stringLength++;
    }

  int jvmStringLength = 0;
  pointer = jvmString;
  while (*pointer != 0)
    {
      pointer++;
      jvmStringLength++;
    }

  if (stringLength != jvmStringLength)
    return 0;

  int i;

#if defined(__MVS__)

  for (i=0; i<stringLength; i++)
    {

      if (E2A[*string] != *jvmString)
        {
          return 0;

        }
      string++;
      jvmString++;
    }

  return 1;

#else

  for (i = 0; i < stringLength; i++)
    {

      if (*string != *jvmString)
        {
          return 0;

        }
      string++;
      jvmString++;
    }

  return 1;

#endif

}

void
findExtensions()
{

  extern jvmtiEnv *globalJVMTIInterface;

  jint numberOfExtensionFunctions;

  jvmtiExtensionFunctionInfo *extensionFunctions;

  getOSThreadID = NULL;

  (*globalJVMTIInterface)->GetExtensionFunctions(globalJVMTIInterface, &numberOfExtensionFunctions, &extensionFunctions);

  int i;
  for (i = 0; i < numberOfExtensionFunctions; i++)
    {

      if (compareJVMString("com.ibm.GetOSThreadID", extensionFunctions[i].id))
        {
          getOSThreadID = extensionFunctions[i].func;
        }

    }

}

void
printExtensions()
{

  //TODO EBCDIC

  extern jvmtiEnv *globalJVMTIInterface;

  jint numberOfExtensionFunctions;

  jvmtiExtensionFunctionInfo *extensionFunctions;

  (*globalJVMTIInterface)->GetExtensionFunctions(globalJVMTIInterface, &numberOfExtensionFunctions, &extensionFunctions);

  int i;

  printf("JVMTI Extension Functions:\n\n");

  for (i = 0; i < numberOfExtensionFunctions; i++)
    {

      printf("\t%d) %s - %s\n", i + 1, extensionFunctions[i].id, extensionFunctions[i].short_description);

      int j;

      if (extensionFunctions[i].param_count > 0)
        {

          printf("\t\t");

          for (j = 0; j < extensionFunctions[i].param_count; j++)
            {

              switch (extensionFunctions[i].params[j].base_type)
                {
              case JVMTI_TYPE_JBYTE:
                printf(JVMTI_TYPE_JBYTE_DESCRIPTION);
                break;
              case JVMTI_TYPE_JCHAR:
                printf(JVMTI_TYPE_JCHAR_DESCRIPTION);
                break;
              case JVMTI_TYPE_JSHORT:
                printf(JVMTI_TYPE_JSHORT_DESCRIPTION);
                break;
              case JVMTI_TYPE_JINT:
                printf(JVMTI_TYPE_JINT_DESCRIPTION);
                break;
              case JVMTI_TYPE_JLONG:
                printf(JVMTI_TYPE_JLONG_DESCRIPTION);
                break;
              case JVMTI_TYPE_JFLOAT:
                printf(JVMTI_TYPE_JFLOAT_DESCRIPTION);
                break;
              case JVMTI_TYPE_JDOUBLE:
                printf(JVMTI_TYPE_JDOUBLE_DESCRIPTION);
                break;
              case JVMTI_TYPE_JBOOLEAN:
                printf(JVMTI_TYPE_JBOOLEAN_DESCRIPTION);
                break;
              case JVMTI_TYPE_JOBJECT:
                printf(JVMTI_TYPE_JOBJECT_DESCRIPTION);
                break;
              case JVMTI_TYPE_JTHREAD:
                printf(JVMTI_TYPE_JTHREAD_DESCRIPTION);
                break;
              case JVMTI_TYPE_JCLASS:
                printf(JVMTI_TYPE_JCLASS_DESCRIPTION);
                break;
              case JVMTI_TYPE_JVALUE:
                printf(JVMTI_TYPE_JVALUE_DESCRIPTION);
                break;
              case JVMTI_TYPE_JFIELDID:
                printf(JVMTI_TYPE_JFIELDID_DESCRIPTION);
                break;
              case JVMTI_TYPE_JMETHODID:
                printf(JVMTI_TYPE_JMETHODID_DESCRIPTION);
                break;
              case JVMTI_TYPE_CCHAR:
                printf(JVMTI_TYPE_CCHAR_DESCRIPTION);
                break;
              case JVMTI_TYPE_CVOID:
                printf(JVMTI_TYPE_CVOID_DESCRIPTION);
                break;
              case JVMTI_TYPE_JNIENV:
                printf(JVMTI_TYPE_JNIENV_DESCRIPTION);
                break;
              default:
                printf("Unknown");
                break;
                }

              switch (extensionFunctions[i].params[j].kind)
                {
              case JVMTI_KIND_IN:
                printf(JVMTI_KIND_IN_DESCRIPTION);
                break;
              case JVMTI_KIND_IN_PTR:
                printf(JVMTI_KIND_IN_PTR_DESCRIPTION);
                break;
              case JVMTI_KIND_IN_BUF:
                printf(JVMTI_KIND_IN_BUF_DESCRIPTION);
                break;
              case JVMTI_KIND_ALLOC_BUF:
                printf(JVMTI_KIND_ALLOC_BUF_DESCRIPTION);
                break;
              case JVMTI_KIND_ALLOC_ALLOC_BUF:
                printf(JVMTI_KIND_ALLOC_ALLOC_BUF_DESCRIPTION);
                break;
              case JVMTI_KIND_OUT:
                printf(JVMTI_KIND_OUT_DESCRIPTION);
                break;
              case JVMTI_KIND_OUT_BUF:
                printf(JVMTI_KIND_OUT_BUF_DESCRIPTION);
                break;
              default:
                break;
                }

              printf(" %s", extensionFunctions[i].params[j].name);

              if (j < (extensionFunctions[i].param_count - 1))
                {
                  printf(", ");
                }

            }

          printf("\n");

        }
      printf("\n");

    }

  jint numberOfExtensionEvents;

  jvmtiExtensionEventInfo *extensionEvents;

  (*globalJVMTIInterface)->GetExtensionEvents(globalJVMTIInterface, &numberOfExtensionEvents, &extensionEvents);

  printf("\nExtension Events (%d)\n", numberOfExtensionEvents);

  for (i = 0; i < numberOfExtensionEvents; i++)
    {
      printf("\t%d) - %s - %s\n", i, extensionEvents[i].id, extensionEvents[i].short_description);
      if (extensionEvents[i].param_count > 0)
        {

          printf("\t\t");
          int j;
          for (j = 0; j < extensionEvents[i].param_count; j++)
            {

              switch (extensionEvents[i].params[j].base_type)
                {
              case JVMTI_TYPE_JBYTE:
                printf(JVMTI_TYPE_JBYTE_DESCRIPTION);
                break;
              case JVMTI_TYPE_JCHAR:
                printf(JVMTI_TYPE_JCHAR_DESCRIPTION);
                break;
              case JVMTI_TYPE_JSHORT:
                printf(JVMTI_TYPE_JSHORT_DESCRIPTION);
                break;
              case JVMTI_TYPE_JINT:
                printf(JVMTI_TYPE_JINT_DESCRIPTION);
                break;
              case JVMTI_TYPE_JLONG:
                printf(JVMTI_TYPE_JLONG_DESCRIPTION);
                break;
              case JVMTI_TYPE_JFLOAT:
                printf(JVMTI_TYPE_JFLOAT_DESCRIPTION);
                break;
              case JVMTI_TYPE_JDOUBLE:
                printf(JVMTI_TYPE_JDOUBLE_DESCRIPTION);
                break;
              case JVMTI_TYPE_JBOOLEAN:
                printf(JVMTI_TYPE_JBOOLEAN_DESCRIPTION);
                break;
              case JVMTI_TYPE_JOBJECT:
                printf(JVMTI_TYPE_JOBJECT_DESCRIPTION);
                break;
              case JVMTI_TYPE_JTHREAD:
                printf(JVMTI_TYPE_JTHREAD_DESCRIPTION);
                break;
              case JVMTI_TYPE_JCLASS:
                printf(JVMTI_TYPE_JCLASS_DESCRIPTION);
                break;
              case JVMTI_TYPE_JVALUE:
                printf(JVMTI_TYPE_JVALUE_DESCRIPTION);
                break;
              case JVMTI_TYPE_JFIELDID:
                printf(JVMTI_TYPE_JFIELDID_DESCRIPTION);
                break;
              case JVMTI_TYPE_JMETHODID:
                printf(JVMTI_TYPE_JMETHODID_DESCRIPTION);
                break;
              case JVMTI_TYPE_CCHAR:
                printf(JVMTI_TYPE_CCHAR_DESCRIPTION);
                break;
              case JVMTI_TYPE_CVOID:
                printf(JVMTI_TYPE_CVOID_DESCRIPTION);
                break;
              case JVMTI_TYPE_JNIENV:
                printf(JVMTI_TYPE_JNIENV_DESCRIPTION);
                break;
              default:
                printf("Unknown");
                break;
                }

              switch (extensionEvents[i].params[j].kind)
                {
              case JVMTI_KIND_IN:
                printf(JVMTI_KIND_IN_DESCRIPTION);
                break;
              case JVMTI_KIND_IN_PTR:
                printf(JVMTI_KIND_IN_PTR_DESCRIPTION);
                break;
              case JVMTI_KIND_IN_BUF:
                printf(JVMTI_KIND_IN_BUF_DESCRIPTION);
                break;
              case JVMTI_KIND_ALLOC_BUF:
                printf(JVMTI_KIND_ALLOC_BUF_DESCRIPTION);
                break;
              case JVMTI_KIND_ALLOC_ALLOC_BUF:
                printf(JVMTI_KIND_ALLOC_ALLOC_BUF_DESCRIPTION);
                break;
              case JVMTI_KIND_OUT:
                printf(JVMTI_KIND_OUT_DESCRIPTION);
                break;
              case JVMTI_KIND_OUT_BUF:
                printf(JVMTI_KIND_OUT_BUF_DESCRIPTION);
                break;
              default:
                break;
                }

              printf(" %s", extensionEvents[i].params[j].name);

              if (j < (extensionEvents[i].param_count - 1))
                {
                  printf(", ");
                }

            }

          printf("\n");

        }
      printf("\n");
    }

  printf("\n");

}

void
createFIFO()
{


  fifo = calloc(1, sizeof(FIFO));
  if (!fifo)
    {
      fprintf(stderr, "Unable to allocate memory\n");
      exit(-1);
    }

  fifo->lock = UNLOCKED;

}

void
queueBuffer(uint32_t bufferLength, uint8_t *buffer)
{

  FIFONode *newNode = calloc(1, sizeof(FIFONode));
  if (!newNode)
    {
      fprintf(stderr, "Unable to allocate memory\n");
      exit(-1);
    }

  newNode->bufferLength = bufferLength;
  newNode->buffer = buffer;

  lock(&fifo->lock);

  if (fifo->rootNode == NULL)
    {

      fifo->rootNode = newNode;

    }
  else
    {

      FIFONode *node = fifo->rootNode;

      while (node->next != NULL)
        {

          node = node->next;

        }

      node->next = newNode;

    }

  unlock(&fifo->lock);


}

void
clearBuffers()
{


  lock(&fifo->lock);

  FIFONode *theNode = fifo->rootNode;

  while (theNode != NULL)
    {

      if (theNode->buffer)
        {
          free(theNode->buffer);
        }

      FIFONode *tempNode = theNode;
      theNode = theNode->next;
      free(tempNode);

    }

  fifo->rootNode = NULL;

  unlock(&fifo->lock);

}

void
networkThread(void *args)
{

  int serverSocket;
  int dataStream = 0;
  struct sockaddr_in serverAddress, clientAddress;

  serverSocket = socket(PF_INET, SOCK_STREAM, 0);

  serverAddress.sin_family = AF_INET;
  serverAddress.sin_addr.s_addr = INADDR_ANY;

  uint32_t port = 12346;

  serverAddress.sin_family = AF_INET;
  serverAddress.sin_addr.s_addr = INADDR_ANY;
  serverAddress.sin_port = htons(port);

  int ret = bind(serverSocket, (struct sockaddr *) &serverAddress, sizeof(serverAddress));

  if (errno == EADDRINUSE)
    {
      fprintf(stderr, "\tPort already in use, allowing host to choose one.\n");
      serverAddress.sin_port = htons(0);

      ret = bind(serverSocket, (struct sockaddr *) &serverAddress, sizeof(serverAddress));
    }

  if (ret != 0)
    {

      fprintf(stderr, "Fatal, unable to bind to port.\n");
      perror("bind");

    }

  socklen_t serverAddressLength = sizeof(serverAddress);

  getsockname(serverSocket, (struct sockaddr *) &serverAddress, &serverAddressLength);

  int portBeingUsed = ntohs(serverAddress.sin_port);

  fprintf(stderr, "\tJinsight is listening on port %d.\n", portBeingUsed);
  fprintf(stderr, "\n");

  listen(serverSocket, 1);

  socklen_t clientAddressSize = sizeof(clientAddress);

  while (alive)
    {
      dataStream = accept(serverSocket, (struct sockaddr *) &clientAddress, &clientAddressSize);

      fprintf(stderr, "Accepting incoming connection:\n");

      char *clientIPAddress = inet_ntoa(clientAddress.sin_addr);

      fprintf(stderr, "\tClient IP address: %s\n", clientIPAddress);
      fprintf(stderr, "\tClient port: %d\n\n", clientAddress.sin_port);
      fflush(stderr);

      clearBuffers();

      clientState = CONNECTED;

        {

          uint32_t bufferLength = sizeof(uint32_t) + 1;

          uint8_t *buffer = malloc(bufferLength);
          uint8_t *pointer = buffer;

          *pointer++ = PROTOCOL_SIZEOF_METHODID;

          uint32_t sizeOfMethodID = sizeof(jmethodID);

          *pointer++ = (uint8_t) (sizeOfMethodID >> 24) & 0xff;
          *pointer++ = (uint8_t) (sizeOfMethodID >> 16) & 0xff;
          *pointer++ = (uint8_t) (sizeOfMethodID >> 8) & 0xff;
          *pointer++ = (uint8_t) sizeOfMethodID & 0xff;

          queueBuffer(bufferLength, buffer);
        }

      while (clientState == CONNECTED)
        {

          pthread_mutex_lock(&moreDataMutex);
          pthread_cond_wait(&moreDataCond, &moreDataMutex);
          pthread_mutex_unlock(&moreDataMutex);


          FIFONode *theNode = NULL;

          lock(&fifo->lock);

          theNode = fifo->rootNode;

          fifo->rootNode = NULL;

          unlock(&fifo->lock);

          while (theNode != NULL)
            {

              if (theNode->buffer)
                {
                  int written = write(dataStream, theNode->buffer, theNode->bufferLength);
                  if (written == -1)
                    clientState = DISCONNECTED;
                  if (theNode->buffer)
                    free(theNode->buffer);
                }

              FIFONode *tempNode = theNode;
              theNode = theNode->next;
              if (tempNode)
                free(tempNode);

            }


          fd_set readfds;
          struct timeval tv;
          FD_ZERO(&readfds);
          FD_SET(dataStream, &readfds);

          tv.tv_sec = 0;
          tv.tv_usec = 10;
          int rv = select(dataStream + 1, &readfds, NULL, NULL, &tv);

          char buf[1];

          if (rv == -1)
            {
              perror("select");
            }
          else if (rv == 0)
            {
              //fprintf(stderr, "No data.\n");
            }
          else
            {
              if (FD_ISSET(dataStream, &readfds))
                {
                  recv(dataStream, &buf, 1, 0);

                  if (buf[0] == COMMAND_FASTER)
                    {
                      hertz++;
                      if (hertz > MAX_HERTZ)
                        hertz = MAX_HERTZ;
                    }
                  if (buf[0] == COMMAND_SLOWER)
                    {
                      hertz--;
                      if (hertz <= MIN_HERTZ)
                        hertz = MIN_HERTZ;
                    }

                }
            }

        }

      clearMethodIDToNameHashTable();

    }

  close(dataStream);
  close(serverSocket);

}

void
stackSamplingThread(void *args)
{

  (*globalJVM)->AttachCurrentThreadAsDaemon(globalJVM, (void **) &localJNIInterface, NULL);

  uint64_t nanoSecondDelay = (1000L / hertz) * 1000000L;

  struct timespec ts;

  ts.tv_sec = 0;
  ts.tv_nsec = nanoSecondDelay;;

  while (alive)
    {

      if (clientState == CONNECTED)
        {
            {

              struct timeval tv;
              gettimeofday(&tv, NULL);

              uint64_t startMicros = (((uint64_t) tv.tv_sec) * ((uint64_t) 1000000)) + (uint64_t) tv.tv_usec;

              jvmtiError returnCode;
              jint numberOfThreads;
              jvmtiStackInfo *callStacks;
              jint depth = 512;

              returnCode = (*globalJVMTIInterface)->GetAllStackTraces(globalJVMTIInterface, depth, &callStacks, &numberOfThreads);
              if (returnCode != JVMTI_ERROR_NONE)
                {
                  fprintf(stderr, "JVMTI error %d calling GetAllStackTraces\n", returnCode);
                }

              uint32_t totalFrames = 0;

              int i;
              for (i = 0; i < numberOfThreads; ++i)
                {

                  jvmtiStackInfo *thisStack = &callStacks[i];

                  totalFrames += thisStack->frame_count;

                  int j;
                  for (j = 0; j < thisStack->frame_count; j++)
                    {

                      jmethodID methodID = thisStack->frame_buffer[j].method;
                      MethodIDToNameNode *theNode = getMethodIDToJinsightClassMethodID(methodID);

                      if (theNode == NULL)
                        {

                          char *methodName;
                          char *methodSignature;
                          char *methodGenericSignature;
                          char *classSignature;
                          char *classGenericSignature;

                          jclass theClass;

                          returnCode = (*globalJVMTIInterface)->GetMethodDeclaringClass(globalJVMTIInterface, thisStack->frame_buffer[j].method, &theClass);
                          if (returnCode != JVMTI_ERROR_NONE)
                            {
                              fprintf(stderr, "JVMTI error %d calling GetMethodDeclaringClass/n", returnCode);
                            }

                          returnCode = (*globalJVMTIInterface)->GetClassSignature(globalJVMTIInterface, theClass, &classSignature, &classGenericSignature);
                          if (returnCode != JVMTI_ERROR_NONE)
                            {
                              fprintf(stderr, "JVMTI error %d calling GetClassSignature/n", returnCode);
                            }

                          returnCode = (*globalJVMTIInterface)->GetMethodName(globalJVMTIInterface, thisStack->frame_buffer[j].method, &methodName, &methodSignature, &methodGenericSignature);
                          if (returnCode != JVMTI_ERROR_NONE)
                            {
                              fprintf(stderr, "JVMTI error %d calling GetMethodName/n", returnCode);
                            }

                          addToMethodIDToNameHashTable(methodID, classSignature, methodName, methodSignature);

                          uint16_t classSignatureLength = strlen(classSignature);
                          uint16_t methodNameLength = strlen(methodName);
                          uint16_t methodSignatureLength = strlen(methodSignature);

                          uint32_t bufferLength = (1 + sizeof(jmethodID) + 2 + classSignatureLength + 2 + methodNameLength + 2 + methodSignatureLength);

                          uint8_t *buffer = malloc(bufferLength);
                          uint8_t *pointer = buffer;

                          *pointer++ = PROTOCOL_DEFINING_METHOD;

                          if (sizeof(jmethodID) == 4)
                            {

                              uint32_t methodID = (uint32_t) thisStack->frame_buffer[j].method;

                              *pointer++ = (uint8_t) (methodID >> 24) & 0xff;
                              *pointer++ = (uint8_t) (methodID >> 16) & 0xff;
                              *pointer++ = (uint8_t) (methodID >> 8) & 0xff;
                              *pointer++ = (uint8_t) methodID & 0xff;

                            }
                          else
                            {

                              uint64_t methodID = (uint64_t) thisStack->frame_buffer[j].method;

                              *pointer++ = (uint8_t) (methodID >> 56) & 0xff;
                              *pointer++ = (uint8_t) (methodID >> 48) & 0xff;
                              *pointer++ = (uint8_t) (methodID >> 40) & 0xff;
                              *pointer++ = (uint8_t) (methodID >> 32) & 0xff;
                              *pointer++ = (uint8_t) (methodID >> 24) & 0xff;
                              *pointer++ = (uint8_t) (methodID >> 16) & 0xff;
                              *pointer++ = (uint8_t) (methodID >> 8) & 0xff;
                              *pointer++ = (uint8_t) methodID & 0xff;
                            }

                          *pointer++ = (uint8_t) (classSignatureLength >> 8) & 0xff;
                          *pointer++ = (uint8_t) classSignatureLength & 0xff;

                          memcpy(pointer, classSignature, classSignatureLength);
                          pointer += classSignatureLength;

                          *pointer++ = (uint8_t) (methodNameLength >> 8) & 0xff;
                          *pointer++ = (uint8_t) methodNameLength & 0xff;

                          memcpy(pointer, methodName, methodNameLength);
                          pointer += methodNameLength;

                          *pointer++ = (uint8_t) (methodSignatureLength >> 8) & 0xff;
                          *pointer++ = (uint8_t) methodSignatureLength & 0xff;

                          memcpy(pointer, methodSignature, methodSignatureLength);
                          pointer += methodSignatureLength;

                          queueBuffer(bufferLength, buffer);

                          (*globalJVMTIInterface)->Deallocate(globalJVMTIInterface, (unsigned char*) methodName);
                          (*globalJVMTIInterface)->Deallocate(globalJVMTIInterface, (unsigned char*) methodSignature);
                          (*globalJVMTIInterface)->Deallocate(globalJVMTIInterface, (unsigned char*) methodGenericSignature);
                          (*globalJVMTIInterface)->Deallocate(globalJVMTIInterface, (unsigned char*) classSignature);
                          (*globalJVMTIInterface)->Deallocate(globalJVMTIInterface, (unsigned char*) classGenericSignature);

                        }

                    }

                }

              uint32_t bufferLength = 1 + 2 + (numberOfThreads * (1 + sizeof(jlong) + sizeof(jint) + 2)) + (totalFrames * sizeof(jmethodID)) + 1;

              uint8_t *buffer = malloc(bufferLength);

              uint8_t *pointer = buffer;

              *pointer++ = PROTOCOL_BEGIN_STACKS;

              *pointer++ = (uint8_t) (numberOfThreads >> 8) & 0xff;
              *pointer++ = (uint8_t) numberOfThreads & 0xff;

              for (i = 0; i < numberOfThreads; ++i)
                {

                  jvmtiStackInfo *thisStack = &callStacks[i];

                  jlong nativeThreadIDLong = 1L;
                  if (getOSThreadID)
                    {
                      returnCode = (*getOSThreadID)(globalJVMTIInterface, thisStack->thread, &nativeThreadIDLong);
                    }

                  *pointer++ = PROTOCOL_THREAD_STACK;

                  *pointer++ = (uint8_t) (nativeThreadIDLong >> 56) & 0xff;
                  *pointer++ = (uint8_t) (nativeThreadIDLong >> 48) & 0xff;
                  *pointer++ = (uint8_t) (nativeThreadIDLong >> 40) & 0xff;
                  *pointer++ = (uint8_t) (nativeThreadIDLong >> 32) & 0xff;
                  *pointer++ = (uint8_t) (nativeThreadIDLong >> 24) & 0xff;
                  *pointer++ = (uint8_t) (nativeThreadIDLong >> 16) & 0xff;
                  *pointer++ = (uint8_t) (nativeThreadIDLong >> 8) & 0xff;
                  *pointer++ = (uint8_t) nativeThreadIDLong & 0xff;

                  uint32_t threadState = (uint32_t) thisStack->state;
                  *pointer++ = (uint8_t) (threadState >> 24) & 0xff;
                  *pointer++ = (uint8_t) (threadState >> 16) & 0xff;
                  *pointer++ = (uint8_t) (threadState >> 8) & 0xff;
                  *pointer++ = (uint8_t) threadState & 0xff;

                  uint16_t numberOfFrames = (uint16_t) thisStack->frame_count;
                  *pointer++ = (uint8_t) (numberOfFrames >> 8) & 0xff;
                  *pointer++ = (uint8_t) numberOfFrames & 0xff;

                  int j;
                  for (j = 0; j < thisStack->frame_count; j++)
                    {

                      if (sizeof(jmethodID) == 4)
                        {

                          uint32_t methodID = (uint32_t) thisStack->frame_buffer[j].method;

                          *pointer++ = (uint8_t) (methodID >> 24) & 0xff;
                          *pointer++ = (uint8_t) (methodID >> 16) & 0xff;
                          *pointer++ = (uint8_t) (methodID >> 8) & 0xff;
                          *pointer++ = (uint8_t) methodID & 0xff;

                        }
                      else
                        {

                          uint64_t methodID = (uint64_t) thisStack->frame_buffer[j].method;

                          *pointer++ = (uint8_t) (methodID >> 56) & 0xff;
                          *pointer++ = (uint8_t) (methodID >> 48) & 0xff;
                          *pointer++ = (uint8_t) (methodID >> 40) & 0xff;
                          *pointer++ = (uint8_t) (methodID >> 32) & 0xff;
                          *pointer++ = (uint8_t) (methodID >> 24) & 0xff;
                          *pointer++ = (uint8_t) (methodID >> 16) & 0xff;
                          *pointer++ = (uint8_t) (methodID >> 8) & 0xff;
                          *pointer++ = (uint8_t) methodID & 0xff;
                        }

                    }

                }

              *pointer++ = PROTOCOL_END_STACKS;

              queueBuffer(bufferLength, buffer);

              for (i = 0; i < numberOfThreads; ++i)
                {
                  jvmtiStackInfo *thisStack = &callStacks[i];
                  (*localJNIInterface)->DeleteLocalRef(localJNIInterface, thisStack->thread);
                }

              (*globalJVMTIInterface)->Deallocate(globalJVMTIInterface, (unsigned char*) callStacks);

              gettimeofday(&tv, NULL);

              uint64_t stopMicros = (((uint64_t) tv.tv_sec) * ((uint64_t) 1000000)) + (uint64_t) tv.tv_usec;

                {
                  uint32_t bufferLength = 1 + sizeof(uint64_t);

                  uint8_t *buffer = malloc(bufferLength);

                  uint8_t *pointer = buffer;

                  uint64_t delay = stopMicros - startMicros;
                  *pointer++ = PROTOCOL_DELAY;
                  *pointer++ = (uint8_t) (delay >> 56) & 0xff;
                  *pointer++ = (uint8_t) (delay >> 48) & 0xff;
                  *pointer++ = (uint8_t) (delay >> 40) & 0xff;
                  *pointer++ = (uint8_t) (delay >> 32) & 0xff;
                  *pointer++ = (uint8_t) (delay >> 24) & 0xff;
                  *pointer++ = (uint8_t) (delay >> 16) & 0xff;
                  *pointer++ = (uint8_t) (delay >> 8) & 0xff;
                  *pointer++ = (uint8_t) delay & 0xff;

                  queueBuffer(bufferLength, buffer);
                }

                {
                  uint32_t bufferLength = 1 + sizeof(uint32_t);

                  uint8_t *buffer = malloc(bufferLength);

                  uint8_t *pointer = buffer;

                  *pointer++ = PROTOCOL_HERTZ;
                  *pointer++ = (uint8_t) (hertz >> 24) & 0xff;
                  *pointer++ = (uint8_t) (hertz >> 16) & 0xff;
                  *pointer++ = (uint8_t) (hertz >> 8) & 0xff;
                  *pointer++ = (uint8_t) hertz & 0xff;

                  queueBuffer(bufferLength, buffer);
                }

              pthread_mutex_lock(&moreDataMutex);
              pthread_cond_signal(&moreDataCond);
              pthread_mutex_unlock(&moreDataMutex);

            }

        }

        nanosleep(&ts, NULL);

    }

}
static void JNICALL
  VMInit(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread)
  {
    globalJNI = jni_env;
    findExtensions();
    createFIFO();
    createMethodIDToNameHashTable();
    alive = true;

    pthread_create(&stackSamplingThreadHandle, NULL, (void*) stackSamplingThread, NULL);
    pthread_create(&networkThreadHandle, NULL, (void*) networkThread, NULL);

  }

  static void JNICALL
  VMDeath(jvmtiEnv *jvmti_env, JNIEnv* jni_env)
  {
    alive = false;
    clientState = DISCONNECTED;

    pthread_mutex_lock(&moreDataMutex);
    pthread_cond_signal(&moreDataCond);
    pthread_mutex_unlock(&moreDataMutex);

    deleteMethodIDToJinsightClassMethodIDHashTable();

    // pthread_cancel(stackSamplingThreadHandle);
    // pthread_cancel(networkThreadHandle);

  }

  JNIEXPORT jint JNICALL
  Agent_OnLoad(JavaVM *jvm, char *options, void *reserved)
  {
#ifdef __MVS__
    createLUTs();
#endif

    pthread_mutex_init(&moreDataMutex, NULL);
    pthread_mutex_init(&networkShutdownMutex, NULL);

    pthread_cond_init(&moreDataCond, NULL);
    pthread_cond_init(&networkShutdownCond, NULL);

    fprintf(stderr, "\nSimpleStack Sampler (c) IBM 2011\n\n");
    fprintf(stderr, "%d\n", sizeof(jmethodID));
    fflush(stderr);

    globalJVM = jvm;

    jvmtiEventCallbacks *eventCallbacks;
    jvmtiError returnCode;

    returnCode = (*jvm)->GetEnv(jvm, (void **) &globalJVMTIInterface, JVMTI_VERSION_1_1);
    if (returnCode < 0)
      {
        fprintf(stderr, "\nThe version of JVMTI requested by SimpleStack (1.1) is not supported by this JVM.\n\n");
        return (JNI_EVERSION);
      }

    eventCallbacks = calloc(1, sizeof(jvmtiEventCallbacks));

    if (!eventCallbacks)
      {
        fprintf(stderr, "Unable to allocate memory");
        exit(JNI_ENOMEM);
      }

    eventCallbacks->VMInit = &VMInit;
    eventCallbacks->VMDeath = &VMDeath;

    returnCode = (*globalJVMTIInterface)->SetEventCallbacks(globalJVMTIInterface, eventCallbacks, (jint) sizeof(*eventCallbacks));
    if (returnCode != JVMTI_ERROR_NONE)
      {
        fprintf(stderr, "JVM does not have the required capabilities (%d)\n", returnCode);
        exit(-1);
      }

    returnCode = (*globalJVMTIInterface)->SetEventNotificationMode(globalJVMTIInterface, JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, (jthread) NULL);
    if (returnCode != JVMTI_ERROR_NONE)
      {
        fprintf(stderr, "JVM does not have the required capabilities, JVMTI_ENABLE, JVMTI_EVENT_VM_INIT (%d)\n", returnCode);
        exit(-1);
      }

    returnCode = (*globalJVMTIInterface)->SetEventNotificationMode(globalJVMTIInterface, JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, (jthread) NULL);
    if (returnCode != JVMTI_ERROR_NONE)
      {
        fprintf(stderr, "JVM does not have the required capabilities, JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH (%d)\n", returnCode);
        exit(-1);
      }

    return JNI_OK;
  }


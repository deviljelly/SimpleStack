#ifndef AGENT_H_
#define AGENT_H_

#include <stdint.h>
#include <jvmti.h>

#define JVMTI_TYPE_JBYTE_DESCRIPTION "jbyte"
#define JVMTI_TYPE_JCHAR_DESCRIPTION "jchar"
#define JVMTI_TYPE_JSHORT_DESCRIPTION "jshort"
#define JVMTI_TYPE_JINT_DESCRIPTION "jint"
#define JVMTI_TYPE_JLONG_DESCRIPTION "jlong"
#define JVMTI_TYPE_JFLOAT_DESCRIPTION "jfloat"
#define JVMTI_TYPE_JDOUBLE_DESCRIPTION "jdouble"
#define JVMTI_TYPE_JBOOLEAN_DESCRIPTION "jboolean"
#define JVMTI_TYPE_JOBJECT_DESCRIPTION "jobject"
#define JVMTI_TYPE_JTHREAD_DESCRIPTION "jthread"
#define JVMTI_TYPE_JCLASS_DESCRIPTION "jclass"
#define JVMTI_TYPE_JVALUE_DESCRIPTION "jvalue"
#define JVMTI_TYPE_JFIELDID_DESCRIPTION "jfieldID"
#define JVMTI_TYPE_JMETHODID_DESCRIPTION "jmethodID"
#define JVMTI_TYPE_CCHAR_DESCRIPTION "char"
#define JVMTI_TYPE_CVOID_DESCRIPTION "void"
#define JVMTI_TYPE_JNIENV_DESCRIPTION "jnienv"

#define JVMTI_KIND_IN_DESCRIPTION "(IN)"
#define JVMTI_KIND_IN_PTR_DESCRIPTION "* (IN_PTR)"
#define JVMTI_KIND_IN_BUF_DESCRIPTION "* (IN_BUF)"
#define JVMTI_KIND_ALLOC_BUF_DESCRIPTION "** (ALLOC_BUF)"
#define JVMTI_KIND_ALLOC_ALLOC_BUF_DESCRIPTION "*** (ALLOC_ALLOC_BUF)"
#define JVMTI_KIND_OUT_DESCRIPTION "* (OUT)"
#define JVMTI_KIND_OUT_BUF_DESCRIPTION "* (OUT_BUF)"

#define PROTOCOL_DEFINING_METHOD 1
#define PROTOCOL_BEGIN_STACKS 2
#define PROTOCOL_THREAD_STACK 3
#define PROTOCOL_END_STACKS 4
#define PROTOCOL_SIZEOF_METHODID 5
#define PROTOCOL_SIZEOF_THREADID 6
#define PROTOCOL_DELAY 7
#define PROTOCOL_HERTZ 8

#define COMMAND_FASTER 10
#define COMMAND_SLOWER 11
#define COMMAND_STOP 12
#define COMMAND_START 13
#define COMMAND_DISCONNECT 14


#define BUFFER_LENGTH 2048
#define DEFAULT_HERTZ 10
#define MAX_HERTZ 250
#define MIN_HERTZ 1

#define METHOD_ID_TO_NAME_HASHTABLE_SIZE 65536
#define METHOD_ID_TO_NAME_HASHTABLE_MASK 65535

#define DISCONNECTED 0
#define CONNECTED 1
#define RUNNING 2

typedef uint32_t lockStructure;


typedef struct FIFO_struct FIFO;
typedef struct FIFONode_struct FIFONode;

typedef struct MethodIDToNameNode_struct MethodIDToNameNode;
typedef struct MethodIDToNameBucket_struct MethodIDToNameBucket;
typedef struct MethodIDToNameHashTable_struct MethodIDToNameHashTable;

struct FIFO_struct
{
  lockStructure lock;
  FIFONode *rootNode;
};

struct FIFONode_struct
{
  uint32_t bufferLength;
  uint8_t *buffer;
  FIFONode *next;
};

struct MethodIDToNameNode_struct
{
  jmethodID methodID;
  char *classSignature;
  char *methodName;
  char *methodSignature;
  MethodIDToNameNode *next;
};

struct MethodIDToNameBucket_struct
{
  lockStructure lock;
  MethodIDToNameNode *rootNode;
};

struct MethodIDToNameHashTable_struct
{
  lockStructure lock;
  uint32_t entries;
  uint32_t collisions;
  MethodIDToNameBucket *buckets[METHOD_ID_TO_NAME_HASHTABLE_SIZE];
};


#ifdef __linux__
# if defined(__s390x__)
#  define LINUX_S390
#  define barrier() __asm__ __volatile__("BR 0": : :"memory")
#  define CS32  "cs"
#  define CS64  "csg"
#  define CSPTR "csg"
# elif defined(__s390__)
#  define LINUX_S390
#  define CS32  "cs"
#  define CS64  "cds"
#  define CSPTR "cs"
# elif defined(__386__)
#  define LINUX_X86
#//  define barrier() __asm__ __volatile__("": : :"memory")
#  define barrier() ;
#  define CS32 "cmpxchgl"
#  define CS64 "cmpxchgq"
#  define CSPTR "cmpxchgl"
# elif defined(__x86_64__)
#  define barrier() __asm__ __volatile__("": : :"memory")
#  define LINUX_X86
#  define CS32 "cmpxchgl"
#  define CS64 "cmpxchgq"
#  define CSPTR "cmpxchgq"
# endif
#elif defined(__MVS__)
# define __builtin_expect(X,Y) X
# define barrier() ((void)getTicks())
# define CS32  __cs1
# ifdef _LP64
#  define CS64  __csg
#  define CSPTR __csg
# else
#  define CS64  __cds1
#  define CSPTR __cs1
# endif
#endif

#ifdef __MVS__
// no nanosleep on z/OS; fake it
static inline int nanosleep(const struct timespec *rqtp, struct timespec *rmtp)
  {
    struct timeval req =
      { .tv_sec = rqtp->tv_sec,
        .tv_usec = rqtp->tv_nsec/1000};

    return select(0, NULL, NULL, NULL, &req);
  }
#endif


static inline uint64_t
getTicks()
{
#ifdef LINUX_X86

  uint32_t lo, hi;
  __asm__ __volatile__("rdtsc"
      : "=a" (lo), "=d" (hi));
  return (uint64_t) hi << 32 | lo;

#elif defined(LINUX_S390)

  unsigned long long time;
  __asm__("stck %[time]"
      : [time] "=Q" (time)
      : : "memory", "cc");
  return time;

#elif defined(__MVS__)

  unsigned long long time;
  __stck(&time);
  return time;

#endif
}

#ifdef LINUX_S390
// CS CC is 0 on success
static inline int compareAndSwap(uint32_t *ptr, uint32_t exp, uint32_t new)
  {
    int ret;
    __asm__ (CS32 " %[exp],%[new],%[ptr]\n\t"
        "ipm %[ret]\n\t"
        "srl %[ret],28"
        : [exp] "+?d" (exp), [ptr] "+?Q" (*ptr), [ret] "=d" (ret)
        : [new] "d" (new)
        : "memory", "cc");
    return ret;
  }

static inline int compareAndSwapPtr(void *ptr, void *exp, void *new)
  {
    int ret;
    __asm__ (
        CSPTR " %[exp],%[new],%[ptr]\n\t"
        "ipm %[ret]\n\t"
        "srl %[ret],28"
        : [exp] "+?d" (exp), [ptr] "+?Q" (*(void**)ptr), [ret] "=d" (ret)
        : [new] "d" (new)
        : "memory", "cc");
    return ret;
  }

#elif defined(LINUX_X86)
static inline int
compareAndSwap(lockStructure *ptr, lockStructure exp, lockStructure new)
{

  lockStructure prev = exp;

  __asm__ __volatile__(
      "lock; " CS32 " %1,%2"
      : "=a"(prev)
      : "r"(new), "m"(*ptr), "0"(prev)
      : "memory");
  return exp != prev; // return zero on success
}

static inline int
compareAndSwapPtr(void *ptr, void *exp, void *new)
{

  uintptr_t *prev = (uintptr_t*) exp; //TODO WARNING .... Test this!!
  uintptr_t* _new = (uintptr_t*) new;
  uintptr_t* _ptr = (uintptr_t*) ptr;

  __asm__ __volatile__(
      "lock; " CSPTR " %1,%2"
      : "=a"(prev)
      : "r"(_new), "m"(*_ptr), "0"(prev)
      : "memory");
  return exp != prev; // return zero on success
}

#elif defined(__MVS__)

#define compareAndSwap(ptr, exp, new) ({ \
          uint32_t exp1=(exp), new1=(new); \
          CS32((void*)&exp1, (ptr), &new1); \
})
#define compareAndSwapPtr(ptr, exp, new) ({ \
          void *exp1=(exp), *new1=(new); \
          CSPTR((void*)&exp1, (ptr), &new1); \
})

#endif

#define UNLOCKED 0
#define LOCKED 1

#define MIN_WAIT 1024
#define MAX_WAIT 1048576


#define hashString jenkins_one_at_a_time_hash

static inline uint32_t
hashPointer(void *ptr) // Thomas Wang, Knuth (32 bit.. Raj did 64 bit magic number)
{
  uintptr_t a = (uintptr_t) ptr;
#ifdef _LP64
  //    a >>= 3; // not sure what these shifts were for... but the hash was not very good
  a *= 11400714819323198485ull; // golden ratio for 2^64
#else
  //    a >>= 2;
  a *= 2654435761ul; // golden ratio for 2^32
#endif
  return a;
}

static inline uint32_t
hashUint32_t(uint32_t a) // Thomas Wang, Knuth
{
  //    a >>= 2;
  a *= 2654435761ul; // golden ratio for 2^32
  return a;
}


static inline uint32_t
hashMethodID(jmethodID id)
{
  return hashPointer(id) & METHOD_ID_TO_NAME_HASHTABLE_MASK;
}


static inline void
unlock(lockStructure *pointer)
{
  *pointer = UNLOCKED;
}


static inline void
lock(lockStructure *pointer)
{ // this seems to be better
  int sleepTime = MIN_WAIT;
  while (compareAndSwap(pointer, UNLOCKED, LOCKED))
    {

      struct timespec ts;

      ts.tv_sec = 0;
      ts.tv_nsec = sleepTime;

      nanosleep(&ts, NULL);

      sleepTime *= 2;
      if (sleepTime >= MAX_WAIT)
        sleepTime = MAX_WAIT;
    }
}

#endif /* AGENT_H_ */

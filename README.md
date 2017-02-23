# Transactional Data Structure Libraries
We introduce transactions into libraries of concurrent data structures; such transactions can be used to ensure atomicity of sequences of data structure operations. By focusing on transactional access to a well-defined set of data structure operations, we strike a balance between the ease-of programming of transactions and the efficiency of customtailored data structures. We exemplify this concept by designing and implementing a library supporting transactions on any number of maps, sets (implemented as skiplists), and queues. Our library offers efficient and scalable transactions, which are an order of magnitude faster than state-of-theart transactional memory toolkits. Moreover, our approach treats stand-alone data structure operations (like put and enqueue) as first class citizens, and allows them to execute with virtually no overhead, at the speed of the original data structure library.

If you use this library, please cite the companion [paper](http://dl.acm.org/citation.cfm?id=2908112&CFID=728652129&CFTOKEN=92618137): A. Spiegelman, G. Golan-Gueta and I. Keidar: Transactional Data Structure Libraries. PLDI 2016. 

This code is an initial work based on the paper. We intend to improve and expand it in the future.

# Usage

This library requires Java 8

```java
LinkedList LL = new LinkedList();
Queue Q = new Queue();
while (true) {
	try {
		try {
			TX.TXbegin();
			LL.put(1, "one");
			LL.containsKey(1);
			LL.get(1);
			Q.isEmpty();
			Q.enqueue(LL.remove(1));
			Q.dequeue();
		} catch (QueueIsEmptyException exp){
			break;
	    } finally {
			TX.TXend();
		}
	} catch (AbortException exp) {
		continue;
	}
	break;
}
```

# Contact

Hagar Porat (hagarp@campus.technion.ac.il)

Guy Gueta (guy.gueta@gmail.com)

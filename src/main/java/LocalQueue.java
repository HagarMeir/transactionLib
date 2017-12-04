public class LocalQueue {

    protected boolean firstDeq = true; // is this the first time of a dequeue?
    protected QNode nodeToDeq = null; // the node to dequeue
    protected boolean isLockedByMe = false; // is queue (not local queue) locked by me
    private QNode head = null;
    private QNode tail = null;
    private int size;

    protected void enqueue(Object val) {

        QNode node = new QNode();
        node.val = val;
        node.next = null;
        node.prev = tail;

        size++;
        if (tail == null) {
            tail = node;
            head = node;

        } else {
            tail.next = node;
            tail = node;
        }
    }

    protected Object dequeue() throws TXLibExceptions.QueueIsEmptyException {

        if (head == null) {
            TXLibExceptions excep = new TXLibExceptions();
            throw excep.new QueueIsEmptyException();
        }
        QNode temp = head;
        Object ret = temp.val;
        head = head.next;
        if (head == null) {
            tail = null;
        } else {
            head.prev = null;
        }
        size--;
        return ret;
    }

    protected boolean isEmpty() {

        return (size <= 0);
    }

}

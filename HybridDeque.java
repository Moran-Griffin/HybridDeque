import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Doubly-linked-list implementation of the java.util.Deque interface. This
 * implementation is more space-efficient than Java's LinkedList class for large
 * collections because each node contains a block of elements instead of only
 * one. This reduces the overhead required for next and previous node
 * references.
 *
 * <p>This implementation does not allow null's to be added to the collection.
 * Adding a null will result in a NullPointerException.
 * 
 * @author Griffin Moran
 * @version 1.0
 * 
 */
public class HybridDeque<E> extends AbstractDeque<E> {
  private static int BLOCK_SIZE = 8;
  private static int CENTER = (BLOCK_SIZE - 1) / 2;

  private Cursor leftCursor;
  private Cursor rightCursor;
  private int size;
  private Block rightBlock;
  private Block leftBlock;
  
  /**
   * Create a HybridDeque instance that contains 2 blocks
   * initially pointing to the same memory space and two cursors that
   * point to elements in the center allowing for the user to add
   * to the left or right first without any issues.
   */
  public HybridDeque() {
    rightBlock = new Block(null, null);
    leftBlock = rightBlock;
    leftCursor = new Cursor(leftBlock, CENTER + 1);
    rightCursor = new Cursor(rightBlock, CENTER);
    size = 0;
  }
  
  /**
   * Clears all values from this HybridDeque.
   * Empty HybridDeques look identical to the HybridDeque created by the constructor.
   */
  public void clear() {
    rightBlock = new Block(null, null);
    leftBlock = rightBlock;
    leftCursor = new Cursor(leftBlock, CENTER + 1);
    rightCursor = new Cursor(rightBlock, CENTER);
    size = 0;
  }
  
  /**
   * Adds an element to the farthest right index in the HybridDeque.
   * If the element is null an exception is thrown.
   */
  public boolean offerLast(E it) {
    if (it == null) {
      throw new NullPointerException();
    }
    if (rightCursor.index == BLOCK_SIZE - 1) {
      rightBlock.next = new Block(rightBlock, null);
      rightBlock.next.prev = rightBlock;
      rightBlock = rightBlock.next;
    }
    rightCursor = rightCursor.next(); 
    rightCursor.set(it);  
    size++;
    return true;
  }
  
  /**
   * Adds an element to the farthest left index in the HybridDeque.
   * If the element is null an exception is thrown.
   */
  public boolean offerFirst(E it) {
    if (it == null) {
      throw new NullPointerException();
    }
    if (leftCursor.index == 0) {
      leftBlock.prev = new Block(null, leftBlock);
      leftBlock.prev.next = leftBlock;
      leftBlock = leftBlock.prev;
    }
    leftCursor = leftCursor.prev(); 
    leftCursor.set(it);  
    size++;
    return true;
  }
  
  /**
   * Returns the farthest left element in the HybridDeque but does not remove it.
   */
  public E peekFirst() {
    if (leftCursor.get() == null) {
      return null;
    }
    return leftCursor.get();
  }
  
  /**
   * Returns the farthest right element in the HybridDeque but does not remove it.
   */
  public E peekLast() {
    if (rightCursor.get() == null) {
      return null;
    }
    return rightCursor.get();
  }
  
  /**
   * Returns and removes the farthest left element in the HybridDeque.
   */
  public E pollFirst() {
    E elem;
    if (leftCursor.index > BLOCK_SIZE - 1) {
      leftCursor = new Cursor(leftBlock.next, 0);
      leftBlock = leftBlock.next;
      leftBlock.prev = null;
    } else {
      leftCursor.next();
    }
    elem = leftCursor.get();
    leftCursor.set(null);
    leftCursor = new Cursor(leftBlock, leftCursor.index + 1);
    size--;
    return elem;
  }
  
  /**
   * Returns and removes the farthest right element in the HybridDeque.
   */
  public E pollLast() {
    E elem;
    if (rightCursor.index < 0) {
      rightCursor = new Cursor(rightBlock.prev, BLOCK_SIZE - 1);
      rightBlock = rightBlock.prev;
      rightBlock.next = null;
    } else {
      rightCursor.prev();
    }
    elem = rightCursor.get();
    rightCursor.set(null);
    rightCursor = new Cursor(rightBlock, rightCursor.index - 1);

    size--;
    return elem;
  }
  
  @Override
  public int size() {
    return size;
  }

  @Override
  public Iterator<E> iterator() {
    return new ForwardIterator();
  }

  @Override
  public Iterator<E> descendingIterator() {
    return new ReverseIterator();
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof AbstractDeque) || this.size() != ((AbstractDeque<E>) other).size()) {
      return false;
    }
    Iterator<E> it = this.iterator();
    Iterator<E> oth = ((HybridDeque<E>) other).iterator();
    while (it.hasNext()) {
      if (!it.next().equals(oth.next())) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * This method is used by all of the other remove methods in HybridDeque,
   * if there is only one element it clears the HybridDeque
   * if the element is the farthest left it just sets the element to null
   * if the element is the furthest right it removes it and moves the rightBlock to the left
   * otherwise it moves each element to the left by 1.
   * 
   * @param cur An alias of the current Cursor to be used
   * @return always returns true, method cannot be called unless the element can be removed
   */
  private boolean removeHelper(Cursor cur) {
    Cursor next = cur.next();
    if (this.size() == 1) {
      this.clear();
      rightCursor = rightCursor.next();
      return true;
    }
    while (cur.get() != null) { //BLOCK_SIZE element in current block
      if (next == null) {
        cur.set(null);
        size--;
        return true;
      }
      if (cur.index == 0 && next.get() == null) { //0 element in current block
        cur.set(null);
        rightBlock = rightBlock.prev;
        rightBlock.next = null;
        rightCursor = new Cursor(rightBlock, BLOCK_SIZE - 1);
        size--;
        return true;
      }
      cur.set(next.get()); //any other element
      cur = next;
      next = cur.next();
    }
    size--;
    return true;
  }
  
  @Override
  public boolean removeFirstOccurrence(Object o) { 
    //make sure that if first is in a separate block if deletes the block
    Cursor cur = leftCursor;
    Cursor next = cur.next();
    while (next != null) { //what if next is null but there is an element inside
      if (cur.get() == null) {
        break;
      }
      if (cur.get().equals(o)) {
        boolean valid = removeHelper(cur);
        if (rightCursor.get() == null) {
          rightCursor = rightCursor.prev();
        }
        return valid;
      }
      cur = next;
      next = cur.next();
    }
    if (cur.get() != null && cur.get().equals(o)) {
      cur.set(null);
      rightCursor = rightCursor.prev();
      size--;
      return true;
    }
    return false;
  }

  @Override
  public boolean removeLastOccurrence(Object o) {
    Cursor cur = rightCursor;
    Cursor prev = cur.prev();
    while (prev != null) {
      if (cur.get() == null) {
        break;
      }
      if (cur.get().equals(o)) {
        boolean valid = removeHelper(cur);
        if (rightCursor.get() == null) {
          rightCursor = rightCursor.prev();
        }
        return valid;
      }
      cur = prev;
      prev = cur.prev();
    }
    if (cur.get() != null && cur.get().equals(o)) {
      cur.set(null);
      leftCursor = leftCursor.next();
      size--;
      return true;
    }
    return false;
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    for (E item : c) {
      this.offerLast(item);
    }
    return false;
  }

  /**
   * DO NOT MODIFY THIS METHOD. This will be used in grading/testing to modify the
   * default block size..
   *
   * @param blockSize The new block size
   */
  protected static void setBlockSize(int blockSize) {
    HybridDeque.BLOCK_SIZE = blockSize;
    HybridDeque.CENTER = (blockSize - 1) / 2;
  }

  private class ForwardIterator implements Iterator<E> {
    private Cursor itCursor = new Cursor(leftBlock, leftCursor.index); 
    private Cursor prev;
    E current;
    boolean canRemove = false;
    
    @Override
    public boolean hasNext() { //add catch to see if there is no block to the right
      if (size == 0) {
        return false;
      }
      if (itCursor != null && itCursor.get() != null) {
        return true;
      }
      return false;
    }
    
    @Override
    public E next() {
      if (hasNext()) {
        canRemove = true;
        current = itCursor.get();
        if (itCursor.index == BLOCK_SIZE - 1) {
          prev = itCursor;
        }
        itCursor = itCursor.next();
        return current;
      }
      throw new NoSuchElementException();
    }
    
    @Override
    public void remove() {
      if (!canRemove) {
        throw new IllegalStateException();
      }
      if (itCursor == null) {
        itCursor = prev;
        removeHelper(itCursor);
      } else {
        removeHelper(itCursor = itCursor.prev());
      }
      if (rightCursor.get() == null) {
        rightCursor = rightCursor.prev();
      }
      canRemove = false;
    }
  }
  
  private class ReverseIterator implements Iterator<E> {
    private Cursor itCursor = new Cursor(rightBlock, rightCursor.index);
    private Cursor prev;
    E current;
    boolean canRemove = false;
    
    @Override 
    public boolean hasNext() {
      if (size == 0) {
        return false;
      }
      if (itCursor != null && itCursor.get() != null) {
        return true;
      }
      return false;
    }

    @Override
    public E next() { //fix itCursor.prev() it keeps pointing to null
      if (hasNext()) {
        canRemove = true;
        current = itCursor.get();
        if (itCursor.index == 0) {
          prev = itCursor;
        }
        itCursor = itCursor.prev();
        return current;
      }
      throw new NoSuchElementException();
    }
    
    @Override
    public void remove() {     
      if (!canRemove) {
        throw new IllegalStateException();
      }
      if (itCursor == null) {
        itCursor = prev;
        removeHelper(itCursor);
      } else {
        removeHelper(itCursor = itCursor.next());
      }
      if (rightCursor.get() == null) {
        rightCursor = rightCursor.prev();
        itCursor = itCursor.prev();
      }
      if (rightCursor.index == BLOCK_SIZE - 1) {
        itCursor = itCursor.prev();
      }
      canRemove = false;
    }
    
  }

  /**
   * Doubly linked list node (or block) containing an array with space for
   * multiple elements.
   */
  private class Block {
    private E[] elements;
    private Block next;
    private Block prev;

    /**
     * Block Constructor.
     *
     * @param prev Reference to previous block, or null if this is the first
     * @param next Reference to next block, or null if this is the last
     */
    @SuppressWarnings("unchecked")
    public Block(Block prev, Block next) {
      this.elements = (E[]) (new Object[BLOCK_SIZE]);
      this.next = next;
      this.prev = prev;
    }
  }

  /**
   * Many of the complications of implementing this Deque class are related to the
   * fact that there are two pieces of information that need to be maintained to
   * track a position in the deque: a block reference and the index within that
   * block. This class combines those two pieces of information and provides the
   * logic for moving forward and backward through the deque structure.
   * 
   * <p>NOTE: The provided cursor class is *immutable*: once a Cursor object is
   * created, it cannot be modified. Incrementing forward or backward involves
   * creating new Cursor objects at the required location. Immutable objects can
   * be cumbersome to work with, but they prevent coding errors caused by
   * accidentally aliasing mutable objects.
   */
  private class Cursor {
    private final Block block;
    private final int index;

    public Cursor(HybridDeque<E>.Block block, int index) {
      this.block = block;
      this.index = index;
    }

    /**
     * Increment the cursor, crossing a block boundary if necessary.
     *
     * @return A new cursor at the next position, or null if there are no more valid
     *         positions.
     */
    private Cursor next() {

      if (index == BLOCK_SIZE - 1) { // We need to cross a block boundary
        if (block.next == null) {
          return null;
        } else {
          return new Cursor(block.next, 0);
        }
      } else { // Just move one spot forward in the current block
        return new Cursor(block, index + 1);
      }
    }

    /**
     * Decrement the cursor, crossing a block boundary if necessary.
     *
     * @return A new cursor at the previous position, or null if there is no
     *         previous position.
     */
    private Cursor prev() {
      if (index == 0) { // We need to cross a block boundary
        if (block.prev == null) {
          return null;
        } else {
          return new Cursor(block.prev, BLOCK_SIZE - 1);
        }
      } else { // Just move one spot back in the current block.
        return new Cursor(block, index - 1);
      }
    }

    /**
     * Return the element stored at this cursor.
     */
    public E get() {
      return block.elements[index];
    }

    /**
     * Set the element at this cursor.
     */
    public void set(E item) {
      block.elements[index] = item;
    }
  }
}
package org.rrd4j.caching;

import java.lang.reflect.Array;
import java.util.Iterator;

/**
 * A array that also provides a LRU access. It should be thread safe.
 * @author Fabrice Bacchella
 *
 * @param <V> the stored type
 */
class LRUArray<V> {

    static final class Payload<V> {
        int key;
        V value;
        Payload(int key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    /** double linked list for LRU **/
    private final DoubleLinkedList<Payload<V>> list;

    /** Array where the item are stored **/
    private final DoubleLinkedList.Node<Payload<V>>[] map;

    /**
     * This sets the size limit.
     * <p>
     * @param maxObjects
     */
    @SuppressWarnings("unchecked")
    public LRUArray( int maxObjects ) {
        list = new DoubleLinkedList<Payload<V>>();
        map = (DoubleLinkedList.Node<Payload<V>>[])Array.newInstance(DoubleLinkedList.Node.class, maxObjects);
    }

    /**
     * This simply returned the number of elements in the array.
     */
    public int size() {
        return map.length;
    }

    /**
     * This removes all the items. It clears the array and the double linked list.
     */
    public void clear() {
        for(int i=0; i < map.length; i++) {
            map[i] = null;
        }
        list.removeAll();
    }

    /**
     * Returns true if the array is empty.
     */
    public boolean isEmpty() {
        return list.size() == 0;
    }

    /**
     * Returns true if the array contains an element for the supplied index.
     */
    public boolean containsKey( int key ) {
        return map[key] != null;
    }

    /**
     * This is an expensive operation that determines if the object supplied is mapped to any index.
     */
    public boolean containsValue( V value ) {
        for(DoubleLinkedList.Node<Payload<V>> n: list) {
            if(n.value.value.equals(value))
                return true;
        }
        return false;
    }

    public Iterable<V> values() {
        final Iterator<DoubleLinkedList.Node<Payload<V>>> i = list.iterator();
        return new Iterable<V>() {
            public Iterator<V> iterator() {
                return new Iterator<V>() {
                    public boolean hasNext() {
                        return i.hasNext();
                    }
                    public V next() {
                        return i.next().value.value;
                    }
                    public void remove() {
                    }
                };
            }
        };
    }

    /**
     * Get the element at the index and update the LRU
     * @param the index
     * @return the element
     */
    public V get(int key) {
        V retVal = null;

        DoubleLinkedList.Node<Payload<V>> me = map[key];

        if ( me != null ) {
            retVal = me.value.value;
            list.makeFirst( me );
        }

        return retVal;
    }

    /**
     * This gets an element out of the array without adjusting it's position in the LRU. In other
     * words, this does not count as being used. If the element is the last item in the list, it
     * will still be the last item in the list.
     * <p>
     * @param key
     * @return Object
     */
    public V getQuiet( int key ) {
        DoubleLinkedList.Node<Payload<V>> me = map[key];
        if ( me != null ) {
            return  me.value.value;
        }
        return null;
    }

    /**
     * Remove the last element in the LRU
     * @return
     */
    public synchronized V removeEldest() {
        return remove(list.getLast().value.key);
    }

    /**
     * Remove an element from the array and return it
     * @param key
     * @return
     */
    public V remove( int key ) {
        // remove single item.
        synchronized (this) {
            DoubleLinkedList.Node<Payload<V>> me = map[key];
            map[key] = null;
            if ( me != null ) {
                list.remove(me);
                return me.value.value;
            }
        }

        return null;
    }

    /**
     * Put the the given element at the index
     * @param key
     * @param value
     * @return the old element
     */
    public V put( int key, V value ) {

        DoubleLinkedList.Node<Payload<V>> old = null;
        synchronized ( this ) {
            // TODO address double synchronization of addFirst, use write lock
            list.addFirst( new Payload<V>(key, value) );
            // this must be synchronized
            old = map[key];
            map[key] =  list.getFirst();

            // If the node was the same as an existing node, remove it.
            if ( old != null && list.getFirst().value.key == old.value.key )
                list.remove( old );
        }

        if ( old != null )
            return old.value.value;
        return null;
    }


    /**
     * Add an new element and make it the oldest
     * @param key
     * @param value
     * @return
     */
    public V putLast( int key, V value ) {

        DoubleLinkedList.Node<Payload<V>> old = null;
        synchronized ( this ) {
            // TODO address double synchronization of addFirst, use write lock
            list.addLast( new Payload<V>(key, value) );
            // this must be synchronized
            old = map[key];
            map[key] =  list.getLast();

            // If the node was the same as an existing node, remove it.
            if ( old != null && list.getLast().value.key == old.value.key )
                list.remove( old );
        }

        if ( old != null )
            return old.value.value;
        return null;
    }

}

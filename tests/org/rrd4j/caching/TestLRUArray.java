package org.rrd4j.caching;

import junit.framework.Assert;

import org.junit.Test;

public class TestLRUArray {
    static final private int numElems = 5;

    @Test
    public void test1() {
        LRUArray<Integer> m = new LRUArray<Integer>(5);
        int i=0;
        m.put(++i % numElems, i);
        m.put(++i % numElems, i);
        m.put(++i % numElems, i);
        m.put(++i % numElems, i);
        m.put(++i % numElems, i);
        m.put(++i % numElems, i);
        Assert.assertEquals("", new Integer(4), m.get(4));
        Assert.assertEquals("", new Integer(2), m.removeEldest());
        m.putLast(++i % numElems, i);
        Assert.assertEquals("", new Integer(i), m.removeEldest());
    }

}

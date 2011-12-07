package org.rrd4j.caching;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestPageCache {
    static final private int numpages = 4;
    //static final private File testFile = new File("tmp/testpagecache");

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws IOException, ParserConfigurationException {
        File libfile = new File(String.format("build/native.%s.%s", System.getProperty("os.name").replaceAll(" ", ""), System.getProperty("os.arch")));
        RrdCachedFileBackendFactory.loadDirect(libfile);
    }

    private byte[] fillBuffer(int size) {
        byte[] buffer = new byte[size];
        for(int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) (i % 127); 
        }
        return buffer;
    }

    private void checkFile() throws IOException {
        File testFile = testFolder.newFile("testpagecache");
        int size =  PageCache.PAGESIZE * numpages;
        Assert.assertEquals("File size invalid", size, testFile.length());
        FileInputStream in = new FileInputStream(testFile);
        byte[] bufferin = new byte[size];
        in.read(bufferin);
        for(int i=0 ; i < size ; i++ ) {
            Assert.assertEquals(String.format("Invalid content at offset %d", i), (byte) (i % 127), bufferin[i]);
        }       
    }

    private void checkFileBypage() throws IOException {
        File testFile = testFolder.newFile("testpagecache");
        Assert.assertEquals("File size invalid", PageCache.PAGESIZE * numpages, testFile.length());
        FileInputStream in = new FileInputStream(testFile);
        byte[] bufferin = new byte[PageCache.PAGESIZE];
        for(int p = 0 ; p < numpages ; p++) {
            in.read(bufferin);
            for(int i=0 ; i < PageCache.PAGESIZE ; i++ ) {
                Assert.assertEquals(String.format("Invalid content at offset %d", i), (byte) (i % 127), bufferin[i]);
            }
        }
    }

    @Test
    public void fillSequential() throws IOException, InterruptedException {
        File testFile = testFolder.newFile("testpagecache");
        PageCache pc = new PageCache(numpages);
        byte[] buffer = fillBuffer(PageCache.PAGESIZE);
        for(byte i= 0; i < numpages; i++) {
            pc.write(testFile, i * PageCache.PAGESIZE, buffer);
        }
        pc.sync();
        checkFileBypage();
    }

    @Test
    public void fillReverse() throws IOException, InterruptedException {
        File testFile = testFolder.newFile("testpagecache");
        PageCache pc = new PageCache(numpages);
        byte[] buffer = fillBuffer(PageCache.PAGESIZE);
        for(int i = numpages - 1 ; i >= 0; i--) {
            pc.write(testFile, i * PageCache.PAGESIZE, buffer);
        }
        pc.sync();
        checkFileBypage();
    }

    @Test
    public void fillOnce() throws IOException, InterruptedException {
        File testFile = testFolder.newFile("testpagecache");
        PageCache pc = new PageCache(numpages);
        byte[] buffer = fillBuffer(PageCache.PAGESIZE * numpages);
        pc.write(testFile, 0, buffer);
        pc.sync();
        checkFile();
    }

    @Test
    public void fillOnceBigger() throws IOException, InterruptedException {
        File testFile = testFolder.newFile("testpagecache");
        PageCache pc = new PageCache(numpages / 2);
        byte[] buffer = fillBuffer(PageCache.PAGESIZE * numpages);
        pc.write(testFile, 0, buffer);
        pc.sync();
        checkFile();
    }

    @Test
    public void readCountFits() throws IOException, InterruptedException {
        File testFile = testFolder.newFile("testpagecache");
        byte[] buffer = new byte[PageCache.PAGESIZE * numpages];
        for(byte i = 0; i < numpages; i++) {
            Arrays.fill(buffer, PageCache.PAGESIZE * i , PageCache.PAGESIZE * (i + 1), (byte)(5 - i));
        }
        FileOutputStream out = new FileOutputStream(testFile);
        out.write(buffer);
        out.flush();
        out.close();
        Arrays.fill(buffer, (byte)0);
        PageCache pc = new PageCache(numpages);
        pc.read(testFile, 0, buffer);
        for(int i = 0; i < buffer.length; i++) {
            Assert.assertEquals("bad read at offset " + i, (byte)(numpages + 1 -  Math.floor(i / PageCache.PAGESIZE)), buffer[i]);
        }
    }

    @Test
    public void readCountOne() throws IOException, InterruptedException {
        File testFile = testFolder.newFile("testpagecache");
        byte[] buffer = fillBuffer(PageCache.PAGESIZE * numpages);
        FileOutputStream out = new FileOutputStream(testFile);
        out.write(buffer);
        out.flush();
        out.close();
        buffer = new byte[1];
        PageCache pc = new PageCache(numpages);
        pc.read(testFile, 0, buffer);
        for(int i = 0; i < PageCache.PAGESIZE * numpages; i++) {
            pc.read(testFile, i, buffer);
            Assert.assertEquals("missmatch at offset " + i, (byte) (i % 127), buffer[0]);
        }
    }

    @Test
    public void readCountBigger() throws IOException, InterruptedException {
        File testFile = testFolder.newFile("testpagecache");
        byte[] buffer = fillBuffer(PageCache.PAGESIZE * numpages);
        FileOutputStream out = new FileOutputStream(testFile);
        out.write(buffer);
        out.flush();
        out.close();
        Arrays.fill(buffer, (byte)0);
        PageCache pc = new PageCache(numpages / 2);
        pc.read(testFile, 0, buffer);
        for(int i = 0; i < buffer.length; i++) {
            Assert.assertEquals("missmatch at offset " + i, (byte) (i % 127), buffer[i]);
        }
    }

}

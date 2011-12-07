package org.rrd4j.caching;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestFilePage {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws IOException, ParserConfigurationException {
        File libfile = new File(String.format("build/native.%s.%s", System.getProperty("os.name").replaceAll(" ", ""), System.getProperty("os.arch")));
        RrdCachedFileBackendFactory.loadDirect(libfile);
    }

    @Test
    public void testWrite() throws IOException {
        File testFile = testFolder.newFile("testfilepage");
        FilePage page = new FilePage(0);
        page.load(testFile, 0);
        page.write(0, getClass().getName().getBytes());
        page.free();

        FileInputStream in = new FileInputStream(testFile);
        byte[] b = new byte[(int) testFile.length()];
        in.read(b);
        Assert.assertEquals("read does not match write", getClass().getName().trim(), new String(b).trim());
    }

    @Test
    public void testRead() throws IOException {
        File testFile = testFolder.newFile("testfilepage");
        FileOutputStream in = new FileOutputStream(testFile);
        in.write(getClass().getName().getBytes());
        in.flush();
        in.close();
        
        FilePage page = new FilePage(0);
        page.load(testFile, 0);
        byte[] b = new byte[(int) testFile.length()];
        page.read(0, b);

        Assert.assertEquals("write does not match read", getClass().getName().trim(), new String(b).trim());
    }
    
    @Test(expected=IOException.class)
    public void testFailed() throws IOException {
        File testFile = testFolder.newFile("testfilepage");
        testFile.delete();
        FilePage.prepare_fd(testFile.getCanonicalPath(), new FileDescriptor(), true);
    }

    @Test(expected=IOException.class)
    public void testFailed2() throws IOException {
        File testFile = testFolder.newFile("testfilepage");
        FileOutputStream out = new FileOutputStream(testFile);
        out.write(0);
        out.close();
        testFile.setReadOnly();
        FilePage.prepare_fd(testFile.getCanonicalPath(), new FileDescriptor(), false);
    }

}

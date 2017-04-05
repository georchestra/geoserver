/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.util;

import com.google.common.io.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@RunWith(PowerMockRunner.class)
@PrepareForTest({ System.class, IOUtils.class})
public class IOUtilsTest {

    TemporaryFolder folder;
    TemporaryFolder temp;

    @Before
    public void initTmpFolder() throws IOException {
        folder = new TemporaryFolder();
        folder.create();
        temp = new TemporaryFolder(new File("target"));
        temp.create();
    }

    @After
    public void deleteTmpFolder() throws IOException {
        folder.delete();
        temp.delete();
    }


    @Test
    public void testZipUnzip() throws IOException {
        Path p1 = temp.newFolder("d1").toPath();
        p1.resolve("foo/bar").toFile().mkdirs();
        Files.touch(p1.resolve("foo/bar/bar.txt").toFile());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(bout);

        IOUtils.zipDirectory(p1.toFile(), zout, null);

        Path p2 = temp.newFolder("d2").toPath();
        p2.toFile().mkdirs();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        IOUtils.decompress(bin, p2.toFile());

        assertTrue(p2.resolve("foo/bar/bar.txt").toFile().exists());
    }

    @Test
    public void renameDirNamesDifferLinux() throws IOException, InterruptedException {
        PowerMockito.mockStatic(System.class);
        Mockito.when(System.getProperty("os.name")).thenReturn("linux");
        String newName = "DirB";

        attemptRename("DirA", newName);

        assertEquals(newName, folder.getRoot().list()[0]);
    }

    @Test
    public void renameDirNamesCaseDifferLinux() throws IOException, InterruptedException {
        PowerMockito.mockStatic(System.class);
        Mockito.when(System.getProperty("os.name")).thenReturn("linux");
        String newName = "Dira";

        attemptRename("DirA", newName);

        assertEquals(newName, folder.getRoot().list()[0]);
    }


    @Test
    public void renameDirNamesDifferWindows() throws IOException, InterruptedException {
        PowerMockito.mockStatic(System.class);
        Mockito.when(System.getProperty("os.name")).thenReturn("Windows");
        String newName = "DirB";

        attemptRename("DirA", newName);

        assertEquals(newName, folder.getRoot().list()[0]);

    }

    @Test
    public void renameDirNamesCaseDifferWindows() throws IOException, InterruptedException {
        PowerMockito.mockStatic(System.class);
        Mockito.when(System.getProperty("os.name")).thenReturn("Windows");
        String oldName = "DirA";
        String newName = "Dira";

        attemptRename("DirA", newName);

        assertEquals(oldName, folder.getRoot().list()[0]);
    }

    private void attemptRename(String oldName, String newName) throws IOException {
        File toBeRenamed = folder.newFolder(oldName);
        assertEquals(1, folder.getRoot().list().length);

        IOUtils.rename(toBeRenamed, newName);

        assertEquals(1, folder.getRoot().list().length);
    }
}

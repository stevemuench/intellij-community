package com.intellij.localvcs;

import org.junit.Test;

public class PathsTest extends TestCase {
  @Test
  public void testParent() {
    assertEquals("dir1/dir2", Paths.getParentOf("dir1/dir2/file"));
    assertNull(Paths.getParentOf("file"));
  }

  @Test
  public void testName() {
    assertEquals("file", Paths.getNameOf("file"));
    assertEquals("file", Paths.getNameOf("dir/file"));
  }

  @Test
  public void testAppending() {
    assertEquals("file1/file2", Paths.appended("file1", "file2"));
  }

  @Test
  public void testAppendingPathWithDriveLetter() {
    assertEquals("c:/root/file", Paths.appended("c:/root", "file"));
  }

  @Test
  public void testRenaming() {
    assertEquals("dir/file2", Paths.renamed("dir/file1", "file2"));
    assertEquals("file2", Paths.renamed("file1", "file2"));
  }

  @Test
  public void testRemovingRoot() {
    assertEquals("file", Paths.withoutRootIfUnder("dir/file", "dir"));

    assertNull(Paths.withoutRootIfUnder("dir/file", "abc"));
    assertNull(Paths.withoutRootIfUnder("dir/file", "di"));

    Paths.setCaseSensitive(true);
    assertNull(Paths.withoutRootIfUnder("dir/file", "DiR"));

    Paths.setCaseSensitive(false);
    assertEquals("file", Paths.withoutRootIfUnder("dir/file", "DiR"));
  }
}

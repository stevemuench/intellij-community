package com.intellij.localvcs;

import java.io.IOException;

public class ChangeFileContentChange extends Change {
  private Content myNewContent;
  private Content myOldContent;
  private Long myNewTimestamp;
  private Long myOldTimestamp;

  public ChangeFileContentChange(String path, Content newContent, Long timestamp) {
    super(path);
    myNewContent = newContent;
    myNewTimestamp = timestamp;
  }

  public ChangeFileContentChange(Stream s) throws IOException {
    super(s);
    myNewContent = s.readContent();
    myOldContent = s.readContent();
    myNewTimestamp = s.readLong();
    myOldTimestamp = s.readLong();
  }

  @Override
  public void write(Stream s) throws IOException {
    super.write(s);
    s.writeContent(myNewContent);
    s.writeContent(myOldContent);
    s.writeLong(myNewTimestamp);
    s.writeLong(myOldTimestamp);
  }

  public Content getNewContent() {
    return myNewContent;
  }

  public Content getOldContent() {
    return myOldContent;
  }

  public Long getNewTimestamp() {
    return myNewTimestamp;
  }

  public Long getOldTimestamp() {
    return myOldTimestamp;
  }

  @Override
  public void applyTo(RootEntry root) {
    Entry affectedEntry = root.getEntry(myPath);

    myOldContent = affectedEntry.getContent();
    myOldTimestamp = affectedEntry.getTimestamp();
    setAffectedIdPath(affectedEntry.getIdPath());

    root.changeFileContent(getAffectedIdPath(), myNewContent, myNewTimestamp);
  }

  @Override
  public void revertOn(RootEntry root) {
    root.changeFileContent(getAffectedIdPath(), myOldContent, myOldTimestamp);
  }
}

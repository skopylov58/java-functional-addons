package com.github.skopylov58.functional.samples;

import org.junit.Test;
import com.github.skopylov58.functional.Validator;

public class BookRecordTest {

  @Test
  public void testBookRecord() throws Exception {
    var bookRec = new BookRecord.Builder("1234", "foo bar")
        .configure(book -> {
          book.author = "author";
          book.description = "desc";
          book.genre = "fiction";
          })
        .build();
    
    System.out.println(bookRec);
    
    Validator.<BookRecord, String>of(bookRec)
    .validate(b -> b.genre().equals("sex") , "not allowed")
    .notNull(BookRecord::author, "Missing author")
    ;
  }
}

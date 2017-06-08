/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.acme.bestpublishing.model;

/**
 * Best Publishing Metadata file format.
 * Naming conventions for metadata file properties.
 * <p/>
 * Book Metadata example:
 *  bookTitle=The Hound of the Baskervilles
 *  bookAuthors=Arthur Conan Doyle
 *  bookGenre=Mystery
 *  nrOfChapters=15
 *  nrOfPages=258
 *
 * Chapter Metadata example:
 *  chapterNumber=1
 *  chapterTitle=The Hound of the Baskervilles
 *  chapterAuthor=Arthur Conan Doyle
 *
 * @author martin.bergljung@marversolutions.org
 * @version 1.0
 */
public class BestPubMetadataFileModel {
    public static final String BOOK_METADATA_TITLE_PROP_NAME = "bookTitle";
    public static final String BOOK_METADATA_GENRE_PROP_NAME = "bookGenre";
    public static final String BOOK_METADATA_AUTHORS_PROP_NAME = "bookAuthors";
    public static final String BOOK_METADATA_NR_OF_CHAPTERS_PROP_NAME = "nrOfChapters";
    public static final String BOOK_METADATA_NR_OF_PAGES_PROP_NAME = "nrOfPages";

    public static final String CHAPTER_METADATA_NUMBER_PROP_NAME = "chapterNumber";
    public static final String CHAPTER_METADATA_TITLE_PROP_NAME = "chapterTitle";
    public static final String CHAPTER_METADATA_AUTHOR_PROP_NAME = "chapterAuthor";

    // Props used for existing chapter folder
    public static final String CHAPTER_FOLDER_NAME_PROP_NAME = "chapterFolderName";
}

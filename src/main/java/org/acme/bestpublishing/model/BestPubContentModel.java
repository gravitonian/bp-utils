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

import org.alfresco.service.namespace.QName;

import java.util.Arrays;
import java.util.List;

/**
 * Best Publishing domain specific content model
 *
 * @author martin.bergljung@marversolutions.org
 */
public class BestPubContentModel {
    public final static String NAMESPACE_URI = "http://www.acme.org/model/content/publishing/1.0";
    public final static String NAMESPACE_PREFIX = "bookpub";

    /**
     * Possible states an ISBN folder can be in when it is populated with incoming content in the Data Dictionary
     */
    public static enum IngestionStatus {
        IN_PROGRESS("In Progress"),
        COMPLETE("Complete");

        private final String status;

        IngestionStatus(final String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return status;
        }
    }

    /**
     * Possible states a book's metadata can be in
     */
    public static enum BookMetadataStatus {
        MISSING("Missing"),  // None of the chapter folders have metadata
        PARTIAL("Partial"),  // Some of the chapter folders have metadata
        COMPLETED("Completed"); // All of the chapter folders have metadata

        private final String status;

        BookMetadataStatus(final String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return status;
        }
    }

    /**
     * Possible states a chapter's metadata can be in

     */
    public static enum ChapterMetadataStatus {
        MISSING("Missing"),
        COMPLETED("Completed");

        private final String status;

        ChapterMetadataStatus(final String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return status;
        }
    }

    /**
     * Book Folder Type
     */
    public static final class BookFolderType {
        public static final QName QNAME = bestpub("bookFolder");

        private BookFolderType() {
        }

        public static final class Prop {
            private Prop() {
            }

            public static final QName INGESTION_STATUS = bestpub("ingestionStatus");
        }
    }

    /**
     * Chapter Folder Type
     */
    public static final class ChapterFolderType {
        public static final QName QNAME = bestpub("chapterFolder");

        private ChapterFolderType() {
        }

        public static final class Prop {
            private Prop() {
            }
        }
    }

    /**
     * Book File Type
     */
    public static final class BookFileType {
        public static final QName QNAME = bestpub("bookFile");

        private BookFileType() {
        }

        public static final class Prop {
            private Prop() {
            }
        }
    }

    /**
     * Chapter File Type
     */
    public static final class ChapterFileType {
        public static final QName QNAME = bestpub("chapterFile");

        private ChapterFileType() {
        }

        public static final class Prop {
            private Prop() {
            }
        }
    }

    /**
     * Artwork File Type
     */
    public static final class ArtworkFileType {
        public static final QName QNAME = bestpub("artworkFile");

        private ArtworkFileType() {
        }

        public static final class Prop {
            private Prop() {
            }
        }
    }

    /**
     * Supplementary File Type
     */
    public static final class SupplementaryFileType {
        public static final QName QNAME = bestpub("supplementaryFile");

        private SupplementaryFileType() {
        }

        public static final class Prop {
            private Prop() {
            }
        }
    }

    /**
     * Book Info metadata Aspect
     */
    public static final class BookInfoAspect {
        public static final QName QNAME = bestpub("bookInfo");

        private BookInfoAspect() {
        }

        public static final class Prop {
            private Prop() {
            }

            public static final QName ISBN = bestpub("ISBN");
            public static final QName BOOK_TITLE = bestpub("bookTitle");
            public static final QName BOOK_GENRE_NAME = bestpub("bookGenre");
            public static final QName BOOK_AUTHORS_NAME = bestpub("bookAuthors");
            public static final QName BOOK_NUMBER_OF_CHAPTERS = bestpub("nrOfChapters");
            public static final QName BOOK_NUMBER_OF_PAGES = bestpub("nrOfPages");
            public static final QName BOOK_METADATA_STATUS = bestpub("bookMetadataStatus");
        }

        public static final List<String> BOOK_GENRE_LIST = Arrays.asList(
                "Non-fiction",
                    "Comedy",
                    "Drama",
                    "Fantasy",
                    "Fiction",
                    "Horror",
                    "Mythology",
                    "Mystery",
                    "Romance",
                    "Satire",
                    "Tragedy",
                    "Tragicomedy"
        );
    }

    /**
     * Chapter Info metadata Aspect
     */
    public static final class ChapterInfoAspect {
        public static final QName QNAME = bestpub("chapterInfo");

        private ChapterInfoAspect() {
        }

        public static final class Prop {
            private Prop() {
            }
            public static final QName CHAPTER_TITLE = bestpub("chapterTitle");
            public static final QName CHAPTER_NUMBER = bestpub("chapterNumber");
            public static final QName CHAPTER_AUTHOR_NAME = bestpub("chapterAuthor");
            public static final QName CHAPTER_METADATA_STATUS = bestpub("chapterMetadataStatus");
        }
    }

    /**
     * Web Publishing Info Aspect
     */
    public static final class WebPublishingInfoAspect {
        public static final QName QNAME = bestpub("webPublishingInfo");

        private WebPublishingInfoAspect() {
        }

        public static final class Prop {
            private Prop() {
            }

            public static final QName WEB_PUBLISHED_DATE = bestpub("webPublishedDate");
            public static final QName WEB_PUBLISHED_VERSION = bestpub("webPublishedVersion");
            public static final QName WEB_SERVER_DELIVERY_OK = bestpub("webServerDeliveryOK");
        }
    }

    public static QName bestpub(final String qname) {
        return QName.createQName(NAMESPACE_URI, qname);
    }
}

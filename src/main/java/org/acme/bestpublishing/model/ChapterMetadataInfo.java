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
 * Chapter metadata information with chapter number, chapter title and author.
 * As extracted from the metadata ZIP text files (e.g. 9780486282145_Chapter_1.txt).
 *
 * @author martin.bergljung@marversolutions.org
 */
public class ChapterMetadataInfo implements Comparable<ChapterMetadataInfo> {

    /**
     * Chapter metadata
     */
    private int chapterNr;
    private String chapterTitle;
    private String chapterAuthor;

    /**
     * The filename for the Text file that was used to populate the chapter metadata.
     */
    private String txtFilename;

    /**
     * Ctor for normal usage where each Text file is contained in a metadata ZIP that is uploaded to Alfresco.
     *
     * @param chapterNr extracted chapter number
     * @param chapterTitle extracted chapter title
     * @param chapterAuthor extracted chapter author
     * @param txtFilename the name of the chapter metadata text file
     */
    public ChapterMetadataInfo(int chapterNr, String chapterTitle, String chapterAuthor, String txtFilename) {
        super();
        this.chapterNr = chapterNr;
        this.chapterTitle = chapterTitle;
        this.chapterAuthor = chapterAuthor;
        this.txtFilename = txtFilename;
    }

    public int getChapterNr() {
        return chapterNr;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public String getChapterAuthor() {
        return chapterAuthor;
    }

    public String getTxtFilename() {
        return txtFilename;
    }

    @Override
    public int compareTo(ChapterMetadataInfo arg0) {
        if (arg0 == null) {
            return -1;
        }
        return chapterNr - arg0.chapterNr;
    }

}

package com.example.application.views.lucene;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Scanner;


import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import oshi.util.FileUtil;

public class LuceneWriteFromFile
{
    public static void main(String[] args)

    {
        //Input folder
        String docsPath = "files";

        //Output folder
        String indexPath = "indexedFiles";
        splitLargeFile("2019-fa","csv",1,false);


        //Input Path Variable
        final Path docDir = Paths.get(docsPath);

        try
        {
            //org.apache.lucene.store.Directory instance
            Directory dir = FSDirectory.open( Paths.get(indexPath) );
            File dirFile = new File("indexedFiles");
            FileUtils.cleanDirectory(dirFile);
            Analyzer analyzer = new StandardAnalyzer();


            //IndexWriter Configuration
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);




            //IndexWriter writes new index files to the directory
            IndexWriter writer = new IndexWriter(dir, iwc);

            //Its recursive method to iterate all files and directories
            indexDocs(writer, docDir);

            writer.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void splitLargeFile(final String fileName,
                                      final String extension,
                                      final int maxLines,
                                      final boolean deleteOriginalFile) {

        String dirName = "index";
        try (Scanner s = new Scanner(new FileReader(String.format("%s.%s", fileName, extension)))) {
            s.useDelimiter("\n");
            int file = 0;
            int cnt = 0;

            BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("files\\%d.%s", file, extension)));

            while (s.hasNext()) {
                writer.write(s.next() );
                if (++cnt == maxLines && s.hasNext()) {
                    writer.close();
                    writer = new BufferedWriter(new FileWriter(String.format("files\\%d.%s", ++file, extension)));
                    cnt = 0;
                }
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (deleteOriginalFile) {
            try {
                File f = new File(String.format("%s.%s", fileName, extension));
                f.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void indexDocs(final IndexWriter writer, Path path) throws IOException
    {
        //Directory?
        if (Files.isDirectory(path))
        {
            //Iterate directory
            Files.walkFileTree(path, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                {
                    try
                    {
                        //Index this file
                        indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
                    }
                    catch (IOException ioe)
                    {
                        ioe.printStackTrace();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        else
        {
            //Index this file
            indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
        }
    }

    static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException
    {
        try (InputStream stream = Files.newInputStream(file))
        {
            //Create lucene Document
            Document doc = new Document();

            doc.add(new TextField("path", file.toString(), Field.Store.YES));
            doc.add(new LongPoint("modified", lastModified));
            doc.add(new TextField("contents", new String(Files.readAllBytes(file)), Store.YES));

            //Updates a document by first deleting the document(s)
            //containing <code>term</code> and then adding the new
            //document.  The delete and then add are atomic as seen
            //by a reader on the same index
            writer.updateDocument(new Term("path", file.toString()), doc);
        }
    }


}


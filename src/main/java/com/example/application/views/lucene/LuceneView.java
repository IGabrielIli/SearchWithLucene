package com.example.application.views.lucene;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import static org.jsoup.internal.StringUtil.isNumeric;


@PageTitle("Lucene")
@Route(value = "Lucene")
@RouteAlias(value = "")
public class LuceneView extends HorizontalLayout {
    private static final String INDEX_DIR = "indexedFiles";
    private String input="2022-sp.csv";
    private TextField name;
    private Button search;
    private String mystr;
    private Button docstats;
    private Button termstats;


     private FileWriter mywriter= new FileWriter("C:\\Users\\hliga\\lucene1-0\\frontend\\results.html");


    public LuceneView() throws IOException {
        name = new TextField("Search here ↓");
        search = new Button("Search");
        docstats=new Button("Document Statistics");
        termstats=new Button("Term Statistics");
        mywriter.write("");


        termstats.addClickListener(e -> {
            try {
                termStatistics("contents");
                UI.getCurrent().getPage().reload();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        docstats.addClickListener(e ->{

            try {
                docStats();
                UI.getCurrent().getPage().reload();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } );

        search.addClickListener(e -> {
            try {
                search(name.getValue());
                UI.getCurrent().getPage().reload();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        search.addClickShortcut(Key.ENTER);

        setMargin(true);
        setVerticalComponentAlignment(Alignment.END, name, search,docstats,termstats);

        add(name, search,docstats,termstats);
    }

    public void search(String st) throws Exception {

        mywriter.write("");



        IndexSearcher searcher = createSearcher(st);
        TopDocs foundDocs = searchInContent(st, searcher);

        mywriter.write("<label>Results : "+foundDocs.totalHits+" of "+foundDocs.totalHits+"</label>");
        String lines="Line,Score,Year,Term,YearTerm,Subject,Number,Name,Description,Credit Hours,Section Info,Degree Attributes,Schedule Information,CRN,Section,Status Code,Part of Term,Section Status,Enrollment Status,Type,Start Time,End Time,Days of Week,Room,Building,Instructors";
        List<String> elephantList1 = new LinkedList<String>(Arrays.asList(lines.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")));

        mywriter.write("<table width=100%>");

        mywriter.write("<tr>");
        for (int i=0; i< elephantList1.size(); i++) {
            mywriter.write("<th>" + elephantList1.get(i) + "</th>");
        }
        mywriter.write("</tr>");
        ;
        String[] que=st.split("[\\s,]+");

        for (ScoreDoc sd : foundDocs.scoreDocs)
        {

            Document d = searcher.doc(sd.doc);

            List<String> elephantList = new LinkedList<String>(Arrays.asList(d.get("contents").split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")));
            elephantList.add(0,Float.toString(sd.score));
            String line1= d.get("path").replace(".csv","");
            String line= line1.replace("files\\","");
            elephantList.add(0,line);


            mywriter.write("<tr>");

            for(int i=0; i<elephantList.size();i++){
                    mywriter.write("<td>");

                String[] cont=elephantList.get(i).split("[\\s,]+");

                for (String b : que){
                    if(cont.length==1){
                        if(cont[0].equalsIgnoreCase(b)){
                            cont[0]="<mark>"+cont[0]+"</mark>";}
                    }else {
                        for (int j=0; j<cont.length; j++){
                            if(cont[j].equalsIgnoreCase(b)){
                                cont[j]="<mark>"+cont[j]+"</mark>";
                            }
                        }
                    }
                }




                for (String a : cont){
                    mywriter.write(a);
                    mywriter.write(" ");
                }
                mywriter.write("</td>");

            }
            System.out.println("Path : "+ d.get("path") + ", Score : " + sd.score+" contents "+d.get("contents"));

            mywriter.write("</tr>");


        }

        mywriter.write("</table>");


        mywriter.close();


    }

    private static TopDocs searchInContent(String textToFind, IndexSearcher searcher) throws Exception
    {
        //Create search query
        QueryParser qp = new QueryParser("contents", new StandardAnalyzer());
        Query query = qp.parse(textToFind);
        QueryScorer queryScorer = new QueryScorer(query, "contents");
        Fragmenter fragmenter = new SimpleSpanFragmenter(queryScorer);

        Highlighter highlighter = new Highlighter(queryScorer);
        highlighter.setTextFragmenter(fragmenter);

        //search the index
        TopDocs hits = searcher.search(query, 50);

        return hits;
    }

    private static IndexSearcher createSearcher(String st) throws IOException
    {

        Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));


        //It is an interface for accessing a point-in-time view of a lucene index
        IndexReader reader = DirectoryReader.open(dir);

        //Index searcher
        IndexSearcher searcher = new IndexSearcher(reader);
        return searcher;
    }

    public void docStats() throws IOException {
        int dstats= -1;
        Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));

        IndexReader reader = DirectoryReader.open(dir);

        dstats=reader.numDocs();

        mywriter.write("");
        mywriter.write("<label><strong>Total Documents: </strong>"+dstats+"</label>");
        mywriter.close();

    }

    public  void termStatistics(String fieldName) throws IOException {
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(INDEX_DIR)));

        List<LeafReaderContext> list = reader.leaves();

        HashMap<String,Long> arr = new HashMap<>();
        for (LeafReaderContext lrc : list) {
            Terms terms = lrc.reader().terms(fieldName);
            if (terms != null) {

                TermsEnum termsEnum = terms.iterator();

                BytesRef term;
                while ((term = termsEnum.next()) != null) {
                    Map<String,Long> frequencyMap = new HashMap<>();
                    String termText = term.utf8ToString();
                    long frequency = termsEnum.totalTermFreq();
                    if(!isNumeric(termText)){
                        if(frequency > 100 && termText.length() > 2){
                            //frequencyMap.put("term" ,termText);
                            frequencyMap.put(termText , frequency);
                            arr.put(termText,frequency);
                        }
                    }
                }

            }

        }

        LinkedHashMap<String,Long> sortedMap = new LinkedHashMap<>();

        arr.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));

        System.out.println(sortedMap);
        LinkedHashMap<String, Long> reverseSortedMap = new LinkedHashMap<>();
        sortedMap.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> reverseSortedMap.put(x.getKey(), x.getValue()));
        System.out.println("Reverse Sorted Map   : " + reverseSortedMap);
        mywriter.write("");
        mywriter.write("<label><strong>Top 10 Terms: </strong></label><br>");

        List list1 = new ArrayList(reverseSortedMap.values());
        List list2 = new ArrayList(reverseSortedMap.keySet());
        int i=0;
        int j=0;
        while(i<10) {
            if (list2.get(j).equals("and") || list2.get(j).equals("the") || list2.get(j).equals("for") || list2.get(j).equals("not") || list2.get(j).equals("or") ){
                j++;
            }else{
                mywriter.write("<label>"+list2.get(j)+" = "+list1.get(j)+"</label><br>");
                i++;
                j++;
            }

        }

        mywriter.close();

    }



}

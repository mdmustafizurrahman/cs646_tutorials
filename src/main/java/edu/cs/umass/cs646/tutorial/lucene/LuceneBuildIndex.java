package edu.cs.umass.cs646.tutorial.lucene;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * This is an example for building a Lucene index for the example corpus.
 *
 * @author Jiepu Jiang (jpjiang@cs.umass.edu)
 * @version 2016-09-10
 */
public class LuceneBuildIndex {
	
	public static void main( String[] args ) {
		try {
			
			String pathCorpus = "/home/jiepu/Downloads/example_corpus.gz";
			String pathIndex = "/home/jiepu/Downloads/example_index_lucene";
			
			Directory dir = FSDirectory.open( new File( pathIndex ).toPath() );
			
			// Analyzer includes options for text processing
			Analyzer analyzer = new Analyzer() {
				@Override
				protected TokenStreamComponents createComponents( String fieldName ) {
					// Step 1: tokenization (Lucene's StandardTokenizer is suitable for most text retrieval occasions)
					TokenStreamComponents ts = new TokenStreamComponents( new StandardTokenizer() );
					// Step 2: transforming all tokens into lowercased ones
					ts = new TokenStreamComponents( ts.getTokenizer(), new LowerCaseFilter( ts.getTokenStream() ) );
					// Step 3: whether to remove stop words
					// Uncomment the following line to remove stop words
					// ts = new TokenStreamComponents( ts.getTokenizer(), new StopwordsFilter( ts.getTokenStream(), StandardAnalyzer.ENGLISH_STOP_WORDS_SET ) );
					// Step 4: whether to apply stemming
					// Uncomment the following line to apply Krovetz or Porter stemmer
					// ts = new TokenStreamComponents( ts.getTokenizer(), new KStemFilter( ts.getTokenStream() ) );
					// ts = new TokenStreamComponents( ts.getTokenizer(), new PorterStemFilter( ts.getTokenStream() ) );
					return ts;
				}
			};
			
			IndexWriterConfig config = new IndexWriterConfig( analyzer );
			// Note that IndexWriterConfig.OpenMode.CREATE will override the original index in the folder
			config.setOpenMode( IndexWriterConfig.OpenMode.CREATE );
			
			IndexWriter ixwriter = new IndexWriter( dir, config );
			
			// This is the field setting for metadata field.
			FieldType fieldTypeMetadata = new FieldType();
			fieldTypeMetadata.setOmitNorms( true );
			fieldTypeMetadata.setIndexOptions( IndexOptions.DOCS );
			fieldTypeMetadata.setStored( true );
			fieldTypeMetadata.setTokenized( false );
			fieldTypeMetadata.freeze();
			
			// This is the field setting for normal text field.
			FieldType fieldTypeText = new FieldType();
			fieldTypeText.setIndexOptions( IndexOptions.DOCS_AND_FREQS_AND_POSITIONS );
			fieldTypeText.setStoreTermVectors( true );
			fieldTypeText.setStoreTermVectorPositions( true );
			fieldTypeText.setTokenized( true );
			fieldTypeText.setStored( true );
			fieldTypeText.freeze();
			
			// You need to iteratively read each document from the corpus file,
			// create a Document object for the parsed document, and add that
			// Document object by calling addDocument().
			
			// Well, the following only works for small text files. DO NOT follow this part in your homework!
			InputStream instream = new GZIPInputStream( new FileInputStream( pathCorpus ) );
			String corpusText = new String( IOUtils.toByteArray( instream ), "UTF-8" );
			instream.close();
			
			Pattern pattern = Pattern.compile(
					"<DOC>.+?<DOCNO>(.+?)</DOCNO>.+?<TITLE>(.+?)</TITLE>.+?<AUTHOR>(.+?)</AUTHOR>.+?<SOURCE>(.+?)</SOURCE>.+?<TEXT>(.+?)</TEXT>.+?</DOC>",
					Pattern.CASE_INSENSITIVE + Pattern.MULTILINE + Pattern.DOTALL
			);
			
			Matcher matcher = pattern.matcher( corpusText );
			
			while ( matcher.find() ) {
				
				String docno = matcher.group( 1 ).trim();
				String title = matcher.group( 2 ).trim();
				String author = matcher.group( 3 ).trim();
				String source = matcher.group( 4 ).trim();
				String text = matcher.group( 5 ).trim();
				
				// Create a Document object
				Document d = new Document();
				// Add each field to the document with the appropriate field type options
				d.add( new Field( "docno", docno, fieldTypeMetadata ) );
				d.add( new Field( "title", title, fieldTypeText ) );
				d.add( new Field( "author", author, fieldTypeText ) );
				d.add( new Field( "source", source, fieldTypeText ) );
				d.add( new Field( "text", text, fieldTypeText ) );
				// Add the document to index.
				ixwriter.addDocument( d );
			}
			
			// remember to close both the index writer and the directory
			ixwriter.close();
			dir.close();
			
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
}

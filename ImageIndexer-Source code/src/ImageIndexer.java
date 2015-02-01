/*Image Indexer
 * 
 * The following class implements the building and storing of an index for a set of images. 
 * For this it considers 10 features, whose extraction is supported by the Lire library. 
 * These features are: Scalable Color, JPEG Coefficients, Color Layout
 * Color Histograms, Tamura, Auto Color Correlogram, CEDD, FCTH, Gabor and Joint Histogram.
 * 
 * This functionality is realized in a required Build Index phase.
 * 		
 * Finally this class allows to perform a query by example over the index, while selecting
 * different features for the similarity measure. This functionality can be executed in the
 * Search phase.
 * 
 * 
 * 
 * In order to do so, this class provides the following functions:
 *  public static void main(String[]): In charge of launching the application. 
 *  public void start(final Stage): Initializing the interface. 
 *  
 *  Functions used for the Build Index phase:
 *  
 *  public void selectImagesDirectory(File): Implementing the selection of the images directory.
 * 	public void buildAndStoreIndex(): Creation and storage of the index, incl. feature extraction.
 * 
 *  Functions used for the Search phase:
 *  public void queryByExample(): Query by example.
 *  
 *  Multimedia Retrieval Prog. Assig. #2
 *  OvGU Magdeburg, SoSe2014
 *  
 *  Shady Akhras, Gabriel Campero.
 *  en.shadiakhras@yahoo.com
 *  gabrielcampero@acm.org
 *  
 * */

//Java libraries
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;

//Lire libraries for feature extraction, building the index and querying.
import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.DocumentBuilderFactory;
import net.semanticmetadata.lire.impl.ChainedDocumentBuilder;
import net.semanticmetadata.lire.ImageSearcher;
import net.semanticmetadata.lire.ImageSearcherFactory;
import net.semanticmetadata.lire.ImageSearchHits;
import net.semanticmetadata.lire.utils.FileUtils;


//Lucene libraries supporting Lire features, most particularly Index storage
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

//Javafx libraries
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ImageIndexer extends Application {
	private TabPane tabPane;
	private Tab indexTab;
	private Tab searchTab;
	private VBox root;
	private VBox tab1Vbox;
	private VBox tab2Vbox;
	private Boolean firstTime;
	boolean indexSet;
	private File indexDir;
	private File imagesDir;
	private File queryImageDir;
	private Button buildIndexButton;
	private Label chooseImageDirectoryMessage;
	private javafx.scene.control.ComboBox<String> featuresComboBox;
	private int maxNumHits;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(final Stage stage) throws Exception {		
		indexSet = false;
		firstTime = true;
		maxNumHits= 100;
		final DirectoryChooser directoryChooserImages = new DirectoryChooser();
		final FileChooser fileChooserQueryImage = new FileChooser();
		final DirectoryChooser directoryChooserIndex = new DirectoryChooser();
		fileChooserQueryImage.getExtensionFilters().setAll(
				new FileChooser.ExtensionFilter("Image Files", "*.png",
						"*.jpg", "*.gif", "*.jpeg", "*.bmp"));
		tabPane = new TabPane();
		indexTab = new Tab("Index");
		searchTab = new Tab("Search");
		searchTab.setDisable(true);
		Label label1 = new Label("Choose images directory");
		Label label2 = new Label("Choose index directory");
		Label label3 = new Label("Choose image");
		final Label label4 = new Label("");
		Button openButtonImages = new Button("Browse");
		Button openButtonIndex = new Button("Browse");
		buildIndexButton = new Button("Build Index");
		Button openButtonQueryByExample = new Button("Browse");
		chooseImageDirectoryMessage = new Label();
		tab1Vbox = new VBox();
		HBox hbox1 = new HBox();
		hbox1.setSpacing(10);
		hbox1.getChildren().add(label2);
		hbox1.getChildren().add(openButtonIndex);
		hbox1.getChildren().add(label4);
		HBox hbox2 = new HBox();
		hbox2.setSpacing(10);
		hbox2.getChildren().addAll(label1, openButtonImages);
		tab1Vbox.setPadding(new Insets(10, 10, 10, 10));
		tab1Vbox.setMinHeight(550);
		tab1Vbox.setSpacing(10);
		tab1Vbox.getChildren().add(hbox1);
		tab1Vbox.getChildren().add(hbox2);
		tab1Vbox.getChildren().add(chooseImageDirectoryMessage);
		tab2Vbox = new VBox();
		HBox hbox3 = new HBox();
		hbox3.setSpacing(10);
		hbox3.getChildren().addAll(label3, openButtonQueryByExample);
		tab2Vbox.setPadding(new Insets(10, 10, 10, 10));
		tab2Vbox.setSpacing(10);
		tab2Vbox.setMinHeight(550);
		tab2Vbox.getChildren().add(hbox3);
		featuresComboBox = new javafx.scene.control.ComboBox<String>();
		featuresComboBox.getItems().addAll("Scalable Color",
				"Auto Color Correlogram", "CEDD", "Color Histogram",
				"Color Layout", "FCTH", "Gabor", "Joint Histogram",
				"JPEG Coefficient Histogram", "Tamura");
		featuresComboBox.setValue("Scalable Color");
		tab2Vbox.getChildren().add(featuresComboBox);
		indexTab.setClosable(false);
		searchTab.setClosable(false);
		indexTab.setContent(tab1Vbox);
		searchTab.setContent(tab2Vbox);
		tabPane.getTabs().add(indexTab);
		tabPane.getTabs().add(searchTab);
		
		openButtonIndex.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent e) {
				File d = directoryChooserIndex.showDialog(stage);
				if (d != null && d.isDirectory() && d.exists()) {
					indexDir = d;
					label4.setText("Image directory: " + d.getAbsolutePath());
				}
			}
		});

		openButtonImages.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent e) {
				File d = directoryChooserImages.showDialog(stage);
				if (d != null) {
					selectImagesDirectory(d);
				}
			}
		});
		openButtonQueryByExample.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent e) {
				queryImageDir = fileChooserQueryImage.showOpenDialog(stage);
				if (queryImageDir != null) {
					try {
						queryByExample();

					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});
		featuresComboBox.valueProperty().addListener(
				new ChangeListener<String>() {
					@Override
					public void changed(@SuppressWarnings("rawtypes") ObservableValue ov, String t, String t1) {

						try {
							queryByExample();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
		buildIndexButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent e) {
				try {
					buildAndStoreIndex();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});

		root = new VBox();
		root.getChildren().add(tabPane);
		Scene scene = new Scene(root, 730, 710);
		stage.setTitle("Image Retrieval - Query By Example");
		stage.setScene(scene);
		stage.show();
	}

	public void selectImagesDirectory(File dir) {
		imagesDir = dir;
		Text dirText = new Text("Images Directory: " + dir.getAbsolutePath());
		int imagesNum = 0;
		int column = -1;
		int row = 0;
		File images[] = imagesDir.listFiles();
		GridPane imagesGrid = new GridPane();
		ScrollPane imagesGridSP = new ScrollPane();
		imagesGridSP.setPrefHeight(450);
		imagesGrid.setHgap(5);
		imagesGrid.setVgap(5);
		imagesGrid.setPadding(new Insets(0, 5, 0, 5));

		for (File imageFile : images) {
			String mimetype = new MimetypesFileTypeMap()
					.getContentType(imageFile);
			String type = mimetype.split("/")[0];
			if (type.equals("image")) {
				imagesNum++;
				Image image = new Image("file:" + imageFile.getAbsolutePath());
				final ImageView imageView = new ImageView(image);
				imageView.setFitWidth(130);
				imageView.setPreserveRatio(true);
				imageView.setSmooth(false);
				imageView.setCache(true);
				column++;
				if (column >= 5) {
					column = 0;
					row++;
				}
				imagesGrid.add(imageView, column, row);
			}
		}
		imagesGridSP.setContent(imagesGrid);

		if (imagesNum == 0) {
			chooseImageDirectoryMessage
					.setText("There are no images in the selected "
							+ dirText.getText());
			chooseImageDirectoryMessage.setTextFill(Color.web("#FF0000"));
		} else {
			chooseImageDirectoryMessage.setText("Number of images: "
					+ imagesNum + " in " + dirText.getText());
			chooseImageDirectoryMessage.setTextFill(Color.web("#000000"));
			tab1Vbox.getChildren().add(imagesGridSP);
			Text indexBuiltText = new Text(
					"Note: Building the index may take a whole minute (please be patient while we extract a lot of features...) .");
			tab1Vbox.getChildren().add(indexBuiltText);
			tab1Vbox.getChildren().add(buildIndexButton);

		}
	}

	public void buildAndStoreIndex() throws IOException, InterruptedException {
		ArrayList<String> images = FileUtils.getAllImages(imagesDir, true);//Array with all found images

		// Creating a CEDD document builder for indexing all files.
		ChainedDocumentBuilder builder = new ChainedDocumentBuilder(); 
		//This type of builder serves as a container for other builders, each
		//supporting a different feature extraction.
		
		builder.addBuilder(DocumentBuilderFactory
				.getAutoColorCorrelogramDocumentBuilder());	
		builder.addBuilder(DocumentBuilderFactory.getCEDDDocumentBuilder());
		builder.addBuilder(DocumentBuilderFactory
				.getColorHistogramDocumentBuilder());
		builder.addBuilder(DocumentBuilderFactory.getColorLayoutBuilder());
		builder.addBuilder(DocumentBuilderFactory.getFCTHDocumentBuilder());
		builder.addBuilder(DocumentBuilderFactory.getGaborDocumentBuilder());
		builder.addBuilder(DocumentBuilderFactory
				.getJointHistogramDocumentBuilder());
		builder.addBuilder(DocumentBuilderFactory
				.getJpegCoefficientHistogramDocumentBuilder());
		builder.addBuilder(DocumentBuilderFactory.getScalableColorBuilder());
		builder.addBuilder(DocumentBuilderFactory.getTamuraDocumentBuilder());
		
		// Creating an Lucene IndexWriter
		IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_48,
				new WhitespaceAnalyzer(Version.LUCENE_48));
		conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		
		IndexWriter iw = new IndexWriter(FSDirectory.open(indexDir), conf);
		
		// Iterating through images extracting features
		for (Iterator<String> it = images.iterator(); it.hasNext();) {
			String imageFilePath = it.next();
			try {
				BufferedImage img = ImageIO.read(new FileInputStream(
						imageFilePath));
				Document document = builder.createDocument(img, imageFilePath); //Here the features are extracted.
				iw.addDocument(document); //Here the Document is stored as an entry of the index.
			} catch (Exception e) {
				Text indexBuiltText = new Text(
						"Error reading image or indexing it.");
				tab1Vbox.getChildren().add(indexBuiltText);
				e.printStackTrace();
			}

		}
		// closing the IndexWriter
		iw.close();
		
		Text indexBuiltText = new Text("Index created."); 
		tab1Vbox.getChildren().add(indexBuiltText);
		searchTab.setDisable(false);
	}
	
	public void queryByExample() throws IOException {
		if (queryImageDir != null && queryImageDir.exists()) {
			if (!firstTime) {
				tab2Vbox.getChildren().remove(
						tab2Vbox.getChildren().toArray().length - 1);
				tab2Vbox.getChildren().remove(
						tab2Vbox.getChildren().toArray().length - 1);
				tab2Vbox.getChildren().remove(
						tab2Vbox.getChildren().toArray().length - 1);
			}
			Text choosenImageText = new Text("Chosen image");
			tab2Vbox.getChildren().add(choosenImageText);
			Image choosenImage = new Image("file:"
					+ queryImageDir.getAbsolutePath());
			final ImageView choosenImageView = new ImageView(choosenImage);
			choosenImageView.setFitWidth(200);
			choosenImageView.setPreserveRatio(true);
			choosenImageView.setSmooth(false);
			choosenImageView.setCache(true);
			HBox hbox = new HBox();
			hbox.getChildren().addAll(choosenImageText, choosenImageView);
			tab2Vbox.getChildren().add(hbox);	
			BufferedImage img = ImageIO.read(queryImageDir);
			GridPane imagesGrid = new GridPane();
			ScrollPane imagesGridSP = new ScrollPane();
			imagesGridSP.setPrefHeight(450);
			imagesGrid.setHgap(5);
			imagesGrid.setVgap(5);
			imagesGrid.setPadding(new Insets(0, 5, 0, 5));
			
			//Lucene object for reading a stored index
			IndexReader ir = DirectoryReader.open(FSDirectory.open(indexDir));
			
			//Lire object for performing a specific search 
			ImageSearcher searcher;
			if (featuresComboBox.getValue().toString().equals("Scalable Color")) {
				searcher = ImageSearcherFactory
						.createScalableColorImageSearcher(maxNumHits);
			} else if (featuresComboBox.getValue().toString().equals(
					"Auto Color Correlogram")) {
				searcher = ImageSearcherFactory
						.createAutoColorCorrelogramImageSearcher(maxNumHits);
			} else if (featuresComboBox.getValue().toString().equals("CEDD")) {
				searcher = ImageSearcherFactory.createCEDDImageSearcher(maxNumHits);
			} else if (featuresComboBox.getValue().toString().equals(
					"Color Histogram")) {
				searcher = ImageSearcherFactory
						.createColorHistogramImageSearcher(maxNumHits);
			} else if (featuresComboBox.getValue().toString().equals(
					"Color Layout")) {
				searcher = ImageSearcherFactory
						.createColorLayoutImageSearcher(maxNumHits);
			} else if (featuresComboBox.getValue().toString().equals("FCTH")) {
				searcher = ImageSearcherFactory.createFCTHImageSearcher(maxNumHits);
			} else if (featuresComboBox.getValue().toString().equals("Gabor")) {
				searcher = ImageSearcherFactory.createGaborImageSearcher(maxNumHits);
			} else if (featuresComboBox.getValue().toString().equals(
					"Joint Histogram")) {
				searcher = ImageSearcherFactory
						.createJointHistogramImageSearcher(maxNumHits);
			} else if (featuresComboBox.getValue().toString().equals(
					"JPEG Coefficient Histogram")) {
				searcher = ImageSearcherFactory
						.createJpegCoefficientHistogramImageSearcher(maxNumHits);
			} else {
				searcher = ImageSearcherFactory.createTamuraImageSearcher(maxNumHits);
			}
			Text selectedFeature = new Text("Using "
					+ featuresComboBox.getValue().toString()
					+ " feature for similarity calculation.");
			tab2Vbox.getChildren().add(selectedFeature);


			ImageSearchHits hits = searcher.search(img, ir); //Here we perform the search itself

			int column = -1;
			int row = 0;
			for (int i = 0; i < hits.length(); i++) {
				String fileName = hits.doc(i).getValues(
						DocumentBuilder.FIELD_NAME_IDENTIFIER)[0];
				Image image = new Image("file:" + fileName);
				final ImageView imageView = new ImageView(image);
				Text similarityText = new Text("Similarity: " + hits.score(i));
				VBox imageAndSim = new VBox();

				imageView.setFitWidth(130);
				imageView.setPreserveRatio(true);
				imageView.setSmooth(false);
				imageView.setCache(true);
				imageAndSim.getChildren().addAll(imageView, similarityText);
				column++;
				if (column >= 5) {
					column = 0;
					row++;
				}
				imagesGrid.add(imageAndSim, column, row);
			}
			imagesGridSP.setContent(imagesGrid);
			tab2Vbox.getChildren().add(imagesGridSP);
			firstTime = false;
		}
	}

	
}
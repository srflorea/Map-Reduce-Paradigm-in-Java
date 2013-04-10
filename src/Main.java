import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

//Clasa in care se creeaza threadurile si in care acestia aduc datele prelucrate
public class Main {

	public static ConcurrentHashMap<String, Vector<SequanceToCompute>> sequances = new ConcurrentHashMap<String, Vector<SequanceToCompute>>();
	public static ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> finalMap = new ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>>();
	
	public static String words[];
	public static String[] documents;
	public static int NC;
	public static int ND;
	public static int D;
	public static int N;
	public static int X;
	
	public static WorkPool wp;
	public static Vector<Worker> workers;
	
	public static Vector<DocumentToVerify> finalSolutions;
	
	//Metoda main a programului
	public static void main(String args[]) {
		
		int nThreads = Integer.parseInt(args[0]);
		wp = new WorkPool(nThreads);
		finalSolutions = new Vector<DocumentToVerify>();
		
		String fileToRead = args[1];
		String fileForWrite = args[2];
		
		//realizeaza citirea din fisierul primit ca parametru in linie de comanda
		readFromFile(fileToRead);
		
		//imparte documentele pentru realizarea etapei Map, in care se vor obtine cuvintele din fiecare document
		partTheDocumentsForTheMapStage();
		
		//se pornesc threadurile pentru realizarea mapului
		startAndJoinTheWorkers(nThreads);
		//se partile determinate mai inainte in work pool pentru prima etapa de reduce
		addPartsInWorkPoolForTheFirstReduceStage();
		//se pornesc threadurile pentru realizarea primei operatii de reduce
		startAndJoinTheWorkers(nThreads);
		//se sorteaza frecventele pentru fiecare document si se trimit pentru efectuarea celei de-a doua etape de reduce
		sortThePartialSolutionsAndPutInWorkPoolForTheSecondReduceStage();
		//se pornesc threadurile pentri realizarea celel de-a doua operatii de reduce
		startAndJoinTheWorkers(nThreads);
		//se scriu datele in fisierul de output
		writeInFile(finalSolutions, fileForWrite);
	}

	private static void sortThePartialSolutionsAndPutInWorkPoolForTheSecondReduceStage() {
		//sortare finalMap
		DocumentToVerify dtv;
		for(ConcurrentHashMap.Entry<String, ConcurrentHashMap<String, Integer>> entryInFinalMap : finalMap.entrySet()) {
			List<Map.Entry<String, Integer>> sortedWords = new ArrayList<Map.Entry<String, Integer>>(entryInFinalMap.getValue().entrySet());
			Collections.sort(sortedWords, new MyComparator());
			
			//numarare numar total de cuvinte din document
			double totalNoOfWords = 0;
			for(Map.Entry<String, Integer> e : sortedWords) {
				totalNoOfWords += e.getValue();
			}
			
			//creare lista cu cele N cuvinte cu frecventa cea mai mare pentru a trimite catre cel de-al doilea reduce
			ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<String, Integer>();
			Map.Entry<String, Integer> entry = null;
			for(int i = N; i > 0; i--) {
				entry = sortedWords.remove(sortedWords.size() - 1);
				map.put(entry.getKey(), entry.getValue());
			}
			Map.Entry<String, Integer> nextEntry = sortedWords.remove(sortedWords.size() - 1);
			while(nextEntry.getValue() == entry.getValue()) {
				map.put(nextEntry.getKey(), nextEntry.getValue());
				nextEntry = sortedWords.remove(sortedWords.size() - 1);
			}
			dtv = new DocumentToVerify(entryInFinalMap.getKey(), (ConcurrentHashMap<String, Integer>)map, new LinkedList<String>(Arrays.asList(words)), totalNoOfWords);
			finalSolutions.add(dtv);
			wp.putWork(dtv);
		}
	}

	private static void addPartsInWorkPoolForTheFirstReduceStage() {
		FrequanciesToUnify ftu;
		for (ConcurrentHashMap.Entry<String, Vector<SequanceToCompute>> entry : sequances.entrySet()) {
			System.out.println(entry.getKey());
				ftu = new FrequanciesToUnify(entry.getKey(), entry.getValue());
				wp.putWork(ftu);
		}
	}

	private static void startAndJoinTheWorkers(int nThreads) {
		workers = new Vector<Worker>();
		Worker worker;
		for(int i = 0; i < nThreads; i++) {
			worker = new Worker(wp);
			worker.start();
			workers.add(worker);
		}
		
		for(int i = 0; i < nThreads; i++) {
			try {
				workers.get(i).join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private static void partTheDocumentsForTheMapStage() {
		for(int i = 0; i < ND; i++) {
			File file = new File(documents[i]);
			int noOfCharactersInFile = (int)file.length();
			
			Vector<SequanceToCompute> sequancesList = new Vector<SequanceToCompute>();
			int charactersComputed = 0;
			SequanceToCompute sequance;
			while(charactersComputed + D - 1 <= noOfCharactersInFile) {
				sequance = new SequanceToCompute(documents[i], noOfCharactersInFile, charactersComputed, charactersComputed + D - 1);
				wp.putWork(sequance);
				sequancesList.add(sequance);
				charactersComputed = charactersComputed + D;
			}
			sequance = new SequanceToCompute(documents[i], noOfCharactersInFile, charactersComputed, noOfCharactersInFile - 1);
			wp.putWork(sequance);
			sequancesList.add(sequance);
			
			sequances.put(documents[i], (Vector<SequanceToCompute>)sequancesList);
		}
	}
	
	private static void readFromFile(String fileToRead) {
		try {
			FileInputStream fStream = new FileInputStream(fileToRead); 
			DataInputStream in = new DataInputStream(fStream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			
			NC = Integer.parseInt(br.readLine());
			words = br.readLine().split(" ");
			D = Integer.parseInt(br.readLine());
			N = Integer.parseInt(br.readLine());
			X = Integer.parseInt(br.readLine());
			ND = Integer.parseInt(br.readLine());
			
			documents = new String[ND];
			for(int i = 0; i < ND; i++) {
				documents[i] = br.readLine();
			}
			
			in.close();
		}
		catch (Exception e) {
			System.err.println("Error: " + e);
		}
	}
	
	private static void writeInFile(Vector<DocumentToVerify> solutions, String fileForWrite) {
		try{
			FileWriter fstream = new FileWriter(fileForWrite);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write("Rezultate pentru: (");
			for(int i = 0; i < words.length; i++) {
				out.write(words[i]);
				if(i != words.length - 1)
					out.write(", ");
			}
			out.write(")");
			out.newLine();
			out.newLine();
			
			for(String document : documents) {
				if(X == 0)
					break;
				for(DocumentToVerify doc : solutions) 
					if(doc.file.equals(document)) {
						if(doc.okToPrint) {
							out.write(doc.file + " (");
							for(int i = 0; i < doc.frequancies.length; i++) {
								out.write(doc.frequancies[i] + "");
								if(i != doc.frequancies.length - 1)
									out.write(", ");
							}
							out.write(")");
							out.newLine();
						}
						break;
					}
				X--;
			}
			out.close();
		  }catch (Exception e){
			  System.err.println("Error: " + e.getMessage());
		  }
	}	
}



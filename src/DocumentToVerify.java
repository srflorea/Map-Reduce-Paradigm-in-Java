import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clasa pentru al treilea tip de date de tipul PartialSolution ce va fi folosit in a doua etapa de reduce
 * @author Razvan
 *
 */
public class DocumentToVerify implements PartialSolution {

	String file;
	Map<String, Integer> mostNFrequentWords;
	List<String> wordsToFind;
	double totalNoOfWords;
	
	double[] frequancies;
	boolean okToPrint;
	
	public DocumentToVerify(String file, ConcurrentHashMap<String, Integer> mostNFrequentWords, LinkedList<String> wordsToFind, double totalNoOfWords) {
		this.file = file;
		this.mostNFrequentWords = mostNFrequentWords;
		this.wordsToFind = wordsToFind;
		this.totalNoOfWords = totalNoOfWords;
		
		this.frequancies = new double[Main.words.length];
		this.okToPrint = false;
	}
}

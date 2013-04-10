import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clasa ce reprezinta o parte din fisier ce va trebui sa fie procesata de un worker. Aceste
 * parti din fisier constituie task-uri care sunt introduse in workpool.
 */
class SequanceToCompute implements PartialSolution {
	
	public String file;
	public int lengthOfFile;
	public int begin;
	public int end;
	
	public List<String> listWithWords;
	public Map<String, Integer> wordsFrequancies;
	
	public SequanceToCompute(String file, int lengthOfFile, int begin, int end) {
		this.file = file;
		this.lengthOfFile = lengthOfFile;
		this.begin = begin;
		this.end = end;
		
		listWithWords = new ArrayList<String>();
		wordsFrequancies = new HashMap<String, Integer>();
	}	
}

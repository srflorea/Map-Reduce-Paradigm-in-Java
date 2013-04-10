import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clasa ce reprezinta un thread worker.
 */
class Worker extends Thread {
	WorkPool wp;

	public Worker(WorkPool workpool) {
		this.wp = workpool;
	}

	/**
	 * Procesarea unei solutii partiale. Aceasta poate implica generarea unor
	 * noi solutii partiale care se adauga in workpool folosind putWork().
	 * Daca s-a ajuns la o solutie finala, aceasta va fi afisata.
	 */
	/**
	 * metoda de procesare a datelor in etapa map
	 * @param sequance Reprezinta tipul de date ce urmeaza a fi procesat
	 */
	
	void processSequance(SequanceToCompute sequance) {
		
		int begin = sequance.begin;
		int end = sequance.end;

		byte[] bytes = new byte[end - begin + 1];
		String[] vector = null;
		try {
			RandomAccessFile raf = new RandomAccessFile(sequance.file, "r");
			
			//se retine caracterul din inaintea acestui fragment de text pentru a sti daca este necesar sa sarim epste anumite caractere
			byte before = 0;
			if(begin != 0) {
				raf.seek(begin - 1);
				before = raf.readByte();
			}
			
			raf.seek(begin);
			
			int j = 0;
			for(j = 0; j < end - begin + 1; j++) {
				bytes[j] = raf.readByte();
			}
			j--;
			byte bef = bytes[j];
			
			for(int i = 0; isLetter(bytes[i]) && begin != 0 && isLetter(before); i++)
				bytes[i] = ' ';
			
			vector = (new String(bytes).split(" |\"|\\||\\;|\\!|\\?|\\>|\\<|\\@|\\#|\\$|\\%|\\^|\\&|\\*|\\_|\\+|\\=|\\{|\\}|\\[|\\]|\\/|\\(|\\)|\\'|\\.|\\,|\\-|\\\n|\\\t|\\\r|\\:|1|2|3|4|5|6|7|8|9|0"));
			
			if(raf.getFilePointer() < sequance.lengthOfFile) {
				byte b = raf.readByte();
				while(isLetter(b) && isLetter(bef)) {
					vector[vector.length - 1] += (char)b;
					b = raf.readByte();
				}
			}

			raf.close();
		}
		catch(Exception e) {
			System.out.println("Error: " + e);
		}			
		
		//se trece vectorul de cuvinte in lista pentru o mai usoara manevrare a acestora
		List<String> list = new LinkedList<String>(Arrays.asList(vector));
		for(int i = 0; i < list.size(); i++)
			if(list.get(i).isEmpty()) {
				list.remove(i);
				i--;
			}
			else  {
				String str = list.get(i).toLowerCase();
				list.remove(i);
				list.add(i, str);
			}
		
		//se innumara frecventele cuvintelor si se pun intr-un map
		Iterator<String> it = list.iterator();
		while(it.hasNext()) {
			int nr = 1;
			String word = it.next();
			if(!sequance.wordsFrequancies.containsKey(word)) {
				for(int i = list.indexOf(word) + 1; i < list.size(); i++) {
					if(word.equals(list.get(i)))
						nr++;
				}
				sequance.wordsFrequancies.put(word, nr);
			}
		}
		
		sequance.listWithWords = (LinkedList<String>)list;
	}
	
	//metoda ce verifica daca un anumit byte este litera
	private boolean isLetter(byte b) {
		if(
				   (b >= 65 && b <= 90)  //este litera mica?
				|| (b >= 97 && b <= 122) //este litera mare?
				)
			return true;
		return false;
	}
	
	/**
	 * metoda de procesare a datelor in prima etapa de reduce
	 * @param sequance Reprezinta tipul de date ce urmeaza a fi procesat
	 */
	void processSequance(FrequanciesToUnify sequance) {
		ConcurrentHashMap<String, Integer> rightMap = Main.finalMap.remove(sequance.file);
		Iterator<SequanceToCompute> it = sequance.sequances.iterator();

			while(it.hasNext()) {
				SequanceToCompute seq = it.next();
				Iterator<Map.Entry<String, Integer>> it2 = seq.wordsFrequancies.entrySet().iterator();
				if(rightMap == null) {
					rightMap = new ConcurrentHashMap<String, Integer>();
					while(it2.hasNext()) {
						Map.Entry<String, Integer> e = it2.next();		
						rightMap.put(e.getKey(), e.getValue());
					}
				}
				else {
					Set<String> set = rightMap.keySet();
					while(it2.hasNext()) {
						Map.Entry<String, Integer> e = it2.next();
						String str = e.getKey();
						if(!set.contains(str)) {
							rightMap.put(str, e.getValue());
						}
						else {
							int freqToBeUpdated = rightMap.remove(str);
							rightMap.put(str, e.getValue() + freqToBeUpdated);
						}
					}
				}
			}
			Main.finalMap.put(sequance.file, (ConcurrentHashMap<String, Integer>)rightMap);
	}
	
	/**
	 * metoda de procesare a datelor din ultima etapa de reduce
	 * @param sequance Reprezinta tipul de date ce urmeaza a fi procesat
	 */
	void processSequance(DocumentToVerify sequance) {
		int i = 0;
		for(String word : sequance.wordsToFind) {
			if(sequance.mostNFrequentWords.containsKey(word)) {
				double freaquancyForWord = sequance.mostNFrequentWords.get(word);
				double frequancyPerCent = freaquancyForWord / sequance.totalNoOfWords * 100;
				BigDecimal finalFreaquancy = new BigDecimal(frequancyPerCent);
				finalFreaquancy = finalFreaquancy.setScale(2, RoundingMode.FLOOR);
				sequance.frequancies[i] = finalFreaquancy.doubleValue();
			}
			else 
				return;
			i++;
		}		
		sequance.okToPrint = true;
	}
	/**
	 * metoda ce va fi apelata la pornirea threadurilor si care va procesa datele din workpool in fucntie de tipul work ce il ia din aceasta
	 */
	public void run() {
		System.out.println("Thread-ul worker " + this.getName() + " a pornit...");
		while (true) {
			PartialSolution ps = wp.getWork();
			if (ps == null)
				break;
			if(ps instanceof SequanceToCompute)
				processSequance((SequanceToCompute)ps);
			else if(ps instanceof FrequanciesToUnify)
				processSequance((FrequanciesToUnify)ps);
			else processSequance((DocumentToVerify)ps);
		}
		System.out.println("Thread-ul worker " + this.getName() + " s-a terminat...");
	}
}
import java.util.Comparator;
import java.util.Map;

/**
 * Clasa ce reprezinta propriul comparator pentru sortarea cuvintelor dupa frecventa
 * @author Razvan
 *
 */
public class MyComparator implements Comparator<Map.Entry<String, Integer>> {

	@Override
	public int compare(Map.Entry<String, Integer> firstEntry, Map.Entry<String, Integer> secondEntry) {
		return firstEntry.getValue() - secondEntry.getValue();
	}
}

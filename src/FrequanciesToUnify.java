import java.util.Vector;

/**
 * Clasa pentru al doilea tip de date de tipul PartialSolution ce va fi folosit in prima etapa de reduce
 * @author Razvan
 *
 */
public class FrequanciesToUnify implements PartialSolution {

	public Vector<SequanceToCompute> sequances;
	public String file;
	
	public FrequanciesToUnify(String file, Vector<SequanceToCompute> sequances) {
		this.file = file;
		this.sequances = sequances; 
	}
}

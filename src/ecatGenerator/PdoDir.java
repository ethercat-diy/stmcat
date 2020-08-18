package ecatGenerator;

public enum PdoDir {
	none,rx,tx;
	static PdoDir parse(String str)
	{
		if (str.length()==0)
			return none;
		else if (str.compareToIgnoreCase("rx")==0)
			return rx;
		else if (str.compareToIgnoreCase("tx")==0)
			return tx;
		else 
			throw new NumberFormatException();
	}
}

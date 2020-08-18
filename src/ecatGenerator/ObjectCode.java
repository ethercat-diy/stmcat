package ecatGenerator;

public enum ObjectCode {
	VARIABLE("OTYPE_VAR"),ARRAY("OTYPE_ARRAY"),RECORD("OTYPE_RECORD");
	private String sOType;
	ObjectCode(String OType_)
	{
		this.sOType = OType_;
	}
	static ObjectCode parse(String str)
	{
		if (str.compareToIgnoreCase("VARIABLE")==0)
			return VARIABLE;
		else if (str.compareToIgnoreCase("ARRAY")==0)
			return ARRAY;
		else if (str.compareToIgnoreCase("RECORD")==0)
			return RECORD;
		else 
			throw new NumberFormatException();
	}
	public String getOType()
	{
		return sOType;
	}
}

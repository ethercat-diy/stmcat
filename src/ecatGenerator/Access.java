package ecatGenerator;

public enum Access {
	RO("ATYPE_RO"),RW("ATYPE_RW");
	private String sAType;
	Access (String sAType_)
	{
		sAType = sAType_;
	}
	static Access parse(String str)
	{
		if (str.compareToIgnoreCase("RO")==0)
			return RO;
		else if (str.compareToIgnoreCase("RW")==0)
			return RW;
		else 
			throw new NumberFormatException();
	}
	public String getAType()
	{
		return sAType;
	}
}

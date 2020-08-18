package ecatGenerator;

import java.util.HashMap;

public enum DataType {
	BOOL("uint16_t", "DTYPE_BOOLEAN", 1),
	BIT1("uint16_t", "DTYPE_BIT1", 1),
	BYTE("uint16_t", "DTYPE_UNSIGNED8", 8),
	SINT("int16_t", "DTYPE_INTEGER8", 8),
	INT("int16_t", "DTYPE_INTEGER16", 16),
	DINT("int32_t", "DTYPE_INTEGER32", 32),
	LINT("int64_t", "DTYPE_INTEGER64", 64),
	USINT("uint16_t", "DTYPE_UNSIGNED8",8),
	UINT("uint16_t", "DTYPE_UNSIGNED16", 16),
	UDINT("uint32_t", "DTYPE_UNSIGNED32", 32),
	ULINT("uint64_t", "DTYPE_UNSIGNED64", 64),
	REAL("float", "DTYPE_REAL32", 32),
	LREAL("double", "DTYPE_REAL64", 64);
	
	private String sTypeC;
	private String sTypeEcat;
	private Integer iBitLen;
	
	DataType(String sTypeC_, String sTypeEcat_, Integer iBitLen_)
	{
		this.sTypeC = sTypeC_;
		this.sTypeEcat = sTypeEcat_;
		this.iBitLen = iBitLen_;
	}
	
	public String getTypeC()
	{
		return sTypeC;
	}
	public String getTypeEcat()
	{
		return sTypeEcat;
	}
	public Integer getBitLen()
	{
		return iBitLen;
	}
	

    private static HashMap<String,DataType> map = new HashMap<String,DataType>();
    static {
        for(DataType d : DataType.values()){
            map.put(d.name(), d);
        }
    }
 
    public static DataType parse(String str) {
        if(map.containsKey(str)){
            return map.get(str);
        }
        else
        	throw new NumberFormatException();
    }

}

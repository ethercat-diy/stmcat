package ecatGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

public class SlaveReader {
    Long lDeviceProfile;
    String sModuleProfile;
    Long lVenderId;
    String sVendorName;
    Long lProductCode;
    Long lRevisionNumber;
    Long lSerialNumber;
    String sDeviceName;
    String sHWVersion;
    String sSWVersion;
    String sGroupType;
    String sGroupName;
    
    List<DictObject> lObject = new ArrayList<DictObject>();

    DictObject objRxPdo = new DictObject(0x1600L,ObjectCode.RECORD, null);
    DictObject objTxPdo = new DictObject(0x1A00L,ObjectCode.RECORD, null);
    {
		//Initialization
		lObject.add(objRxPdo);
		lObject.add(objTxPdo);
    }

    
    static public Long getLongCellValue(XSSFCell cell)
    {
    	long ret;
    	Pattern pattern = Pattern.compile("[0-9]*");
    	if (cell==null)
    		return Long.MIN_VALUE;
    	if(cell.getCellType()==CellType.NUMERIC)
    		ret = (long) cell.getNumericCellValue();
    	else if (cell.getCellType()==CellType.STRING)
    	{
    		try {
	    		String str = cell.getStringCellValue();
	    		if (str.substring(0, 2).compareToIgnoreCase("0x")==0)
	    			ret = Long.parseLong(str.substring(2),16);
	    		else if (pattern.matcher(str).matches())
	    			ret = Long.parseLong(str.substring(2),10);
	    		else
	    			ret = Long.MIN_VALUE;
    		}
    		catch(NumberFormatException e)
    		{
    			e.printStackTrace();
    			ret = Long.MIN_VALUE;
    		}
    	}
    	else
    		ret = Long.MIN_VALUE;
    	return ret;
    }
    
    static public String getStringCellValue(XSSFCell cell)
    {
    	if (cell==null)
    		return new String("");
    	else
    	{
    		cell.setCellType(CellType.STRING);
    	   	return cell.getStringCellValue();
    	}
    }
    
    SlaveReader(XSSFSheet mySheet)
    {
    	//Read XLSX    	
    	XSSFRow row;
    	//Read basic info
    	readSlaveInfo(mySheet);
    	//Read Objects
    	int nrows = mySheet.getLastRowNum();
    	for (int i=17;i<=nrows;i++)
    	{
    		row = mySheet.getRow(i);
    		//Skip empty items (Index and SI not valid number)
    		long lIndex = getLongCellValue(row.getCell(1));
    		long lSI = getLongCellValue(row.getCell(3));
    		if (lIndex==Long.MIN_VALUE && lSI==Long.MIN_VALUE )
    			continue;
    		//If First Object, then Index must be valid
    		if (lObject.isEmpty() && lIndex==Long.MIN_VALUE)
    			SlaveGenerator.consoleStream.println(new String("Error: Row").concat(Integer.toString(row.getRowNum()))
						.concat(". Index should not be empty."));
    		//Else if no index, or index == previous, append entry
    		else if (lIndex==Long.MIN_VALUE || (!lObject.isEmpty() && lIndex == lObject.get(lObject.size()-1).lIndex ))
    			lObject.get(lObject.size()-1).append(row);
    		//Else. has index, and index != previos
    		else
    			lObject.add(new DictObject(row));
    	}
    	generatePdo();
    }
    
    void readSlaveInfo(XSSFSheet mySheet)
    {
    	XSSFRow row;
    	XSSFCell cell;
    	row = mySheet.getRow(0);cell = row.getCell(2);
    	lDeviceProfile = getLongCellValue(cell);
    	row = mySheet.getRow(2);cell = row.getCell(2);
    	lVenderId = getLongCellValue(cell);
    	row = mySheet.getRow(3);cell = row.getCell(2);
    	sVendorName = getStringCellValue(cell);
    	row = mySheet.getRow(4);cell = row.getCell(2);
    	lProductCode = getLongCellValue(cell);
    	row = mySheet.getRow(5);cell = row.getCell(2);
    	lRevisionNumber = getLongCellValue(cell);
    	row = mySheet.getRow(6);cell = row.getCell(2);
    	lSerialNumber = getLongCellValue(cell);
    	row = mySheet.getRow(7);cell = row.getCell(2);
    	sDeviceName = getStringCellValue(cell);
    	row = mySheet.getRow(8);cell = row.getCell(2);
    	sHWVersion = getStringCellValue(cell);
    	row = mySheet.getRow(9);cell = row.getCell(2);
    	sSWVersion = getStringCellValue(cell);
    	row = mySheet.getRow(10);cell = row.getCell(2);
    	sGroupType = getStringCellValue(cell);
    	row = mySheet.getRow(11);cell = row.getCell(2);
    	sGroupName = getStringCellValue(cell);
    }
    
    public HashMap<String,String> generateMapC()
    {
    	HashMap<String,String> ret = new HashMap<String,String>();
    	ret.put("%(Device Type)", Long.toHexString(lDeviceProfile));
    	ret.put("%(Vendor ID)", Long.toHexString(lVenderId));
    	ret.put("%(Product Code)", Long.toHexString(lProductCode));
    	ret.put("%(Revision Number)", Long.toHexString(lRevisionNumber));
    	ret.put("%(Serial Number)", Long.toHexString(lSerialNumber));
    	ret.put("%(Device Name)", sDeviceName);
    	ret.put("%(Hardware Version)", sHWVersion);
    	ret.put("%(Software Version)", sSWVersion);
    	ret.put("%(Group Type)", sGroupType);
    	ret.put("%(Group Name)", sGroupName);
    	ret.put("%(Custom acName)", generateAcString());
    	ret.put("%(Custom SDO)", generateSDO());
    	ret.put("%(nRxPdo)", Integer.toString(objRxPdo.lEntry.size()-1));
    	ret.put("%(nTxPdo)", Integer.toString(objTxPdo.lEntry.size()-1));
    	ret.put("%(Object List)", generateObjList());
   	return ret;
    }
    
    public void generatePdo()
    {
		String sPdoName = new String("TXPDO");
		objTxPdo.lEntry.add(new DictEntry(0L,DataType.BYTE,sPdoName,sPdoName,new String("0"),Access.RO,PdoDir.none,new String("")));
		sPdoName = new String("RXPDO");
		objRxPdo.lEntry.add(new DictEntry(0L,DataType.BYTE,sPdoName,sPdoName,new String("0"),Access.RO,PdoDir.none,new String("")));
		
		Long idRxPdo = new Long(1L), idTxPdo = new Long(1L);
    	for (DictObject object : lObject)
    	{
    		DictObject pdoObject;
    		Long id;
    		if (object.lIndex>=0x6000 && object.lIndex<=0x6999)
    		{
    			pdoObject = objTxPdo;
    			id = idTxPdo;
    			sPdoName = new String("TXPDO");
    		}
    		else if (object.lIndex>=0x7000 && object.lIndex<=0x7999)
    		{
    			pdoObject = objRxPdo;
    			id = idRxPdo;
    			sPdoName = new String("RXPDO");
    		}
    		else
    			continue;
    		String sEntryName, sDefault;
    		switch (object.eObjectCode)
    		{
    		case RECORD:
	    		for (DictEntry entry: object.lEntry)
	    		{
	    			if(entry.lSI==0)	continue;
	    			sEntryName = sPdoName.concat("_").concat(id.toString());
	    			sDefault = new String("0x").concat(Long.toHexString((object.lIndex<<16)+(entry.lSI<<8)+(entry.dataType.getBitLen())));
	    			pdoObject.lEntry.add(new DictEntry(id++,DataType.UDINT,sEntryName,sEntryName,sDefault,Access.RO,PdoDir.none,new String("")));
	    		}
	    		break;
    		case VARIABLE:
    			sEntryName = sPdoName.concat("_").concat(id.toString());
    			DictEntry entry = object.lEntry.get(0);
    			sDefault = new String("0x").concat(Long.toHexString((object.lIndex<<16)+(entry.lSI<<8)+(entry.dataType.getBitLen())));
    			pdoObject.lEntry.add(new DictEntry(id++,DataType.UDINT,sEntryName,sEntryName,sDefault,Access.RO,PdoDir.none,new String("")));
    			break;
    		case ARRAY:
    			Integer n = Integer.parseInt(object.lEntry.get(0).sDefault);
    			for (Integer i=1;i<=n;i++)
    			{
        			sEntryName = sPdoName.concat("_").concat(id.toString());
    				entry = object.lEntry.get(1);
        			sDefault = new String("0x").concat(Long.toHexString((object.lIndex<<16)+(i<<8)+(entry.dataType.getBitLen())));
        			pdoObject.lEntry.add(new DictEntry(id++,DataType.UDINT,sEntryName,sEntryName,sDefault,Access.RO,PdoDir.none,new String("")));
    			}
    			break;
    		}
    		if (object.lIndex>=0x6000 && object.lIndex<=0x6999)
    			idTxPdo = id;
    		else if (object.lIndex>=0x7000 && object.lIndex<=0x7999)
    			idRxPdo = id;
    	}
    	objRxPdo.lEntry.get(0).sDefault = Integer.toString(objRxPdo.lEntry.size()-1);
    	objTxPdo.lEntry.get(0).sDefault = Integer.toString(objTxPdo.lEntry.size()-1);
    }
    
    public String generateUserObjects()
    {
    	String ret = "";
    	for (DictObject object : lObject) {
			switch (object.eObjectCode)
			{
			case RECORD:
	    		ret = ret.concat("\tstruct {\n");
				for(DictEntry entry: object.lEntry)
				{
					if (entry.lSI==0) continue;
					ret = ret.concat("\t\t");
					ret = ret.concat(entry.dataType.getTypeC()).concat(" ");
					ret = ret.concat(entry.sVarName).concat(";\n");
				}
				ret = ret.concat(new String("\t} ").concat(object.lEntry.get(0).sVarName)).concat(";\n");
				break;
			case VARIABLE:
				ret = ret.concat("\t");
				DictEntry entry =  object.lEntry.get(0);
				ret = ret.concat(entry.dataType.getTypeC()).concat(" ");
				ret = ret.concat(entry.sVarName).concat(";\n");
				break;
			case ARRAY:
				ret = ret.concat("\t");
				ret = ret.concat(object.lEntry.get(1).dataType.getTypeC()).concat(" ");
				ret = ret.concat(object.lEntry.get(1).sVarName).concat("[");
				ret = ret.concat(object.lEntry.get(0).sDefault).concat("];\n");
				break;
			default:
				break;
			}
		}
    	return ret;
    }
    
    public String generateAcString()
    {
    	String ret = "";
    	for (DictObject object : lObject) {
    		ret = ret.concat("static const char acName");
    		ret = ret.concat(Long.toHexString(object.lIndex)).concat("[] = \"");
    		ret = ret.concat(object.lEntry.get(0).sName).concat("\";\n");
    		switch (object.eObjectCode)
			{
			case RECORD:
	    		ret = ret.concat("static const char acName");
	    		ret = ret.concat(Long.toHexString(object.lIndex)).concat("_0[] = \"Max SubIndex\";\n");
				for(DictEntry entry: object.lEntry)
				{
					if (entry.lSI==0) continue;
					ret = ret.concat("static const char acName");
		    		ret = ret.concat(Long.toHexString(object.lIndex)).concat("_");
		    		ret = ret.concat(entry.lSI.toString()).concat("[] = \"");
		    		ret = ret.concat(entry.sName).concat("\";\n");
				}
				break;
			case VARIABLE:
				break;
			case ARRAY:
	    		ret = ret.concat("static const char acName");
	    		ret = ret.concat(Long.toHexString(object.lIndex)).concat("_0[] = \"Max SubIndex\";\n");
	    		for (Integer i = 1;i<=Integer.parseInt(object.lEntry.get(0).sDefault);i++)
	    		{
					ret = ret.concat("static const char acName");
		    		ret = ret.concat(Long.toHexString(object.lIndex)).concat("_");
		    		ret = ret.concat(i.toString()).concat("[] = \"");
		    		ret = ret.concat(object.lEntry.get(1).sName).concat(i.toString()).concat("\";\n");
	    		}
			}
    	}
    	return ret;
    }
    
    public String generateSDO() {
    	String ret = "";
    	for (DictObject object : lObject) {
    		ret = ret.concat("const _objd SDO");
    		ret = ret.concat(Long.toHexString(object.lIndex)).concat("[] = \n{\n");
    		switch (object.eObjectCode)
			{
			case RECORD:
				for(DictEntry entry: object.lEntry)
				{
					ret = ret.concat("\t{");
					if (entry.lSI==0) 
					{
						ret = ret.concat("0, DTYPE_UNSIGNED8, 8, ATYPE_RO, acName");
						ret = ret.concat(Long.toHexString(object.lIndex)).concat("_0,");
						ret = ret.concat(Integer.toString(object.lEntry.size()-1)).concat(", NULL");
					}
					else
					{
						ret = ret.concat(entry.lSI.toString()).concat(", ");
						ret = ret.concat(entry.dataType.getTypeEcat()).concat(", ");
						ret = ret.concat(entry.dataType.getBitLen().toString()).concat(", ");
						ret = ret.concat(entry.access.getAType()).concat(", acName");
						ret = ret.concat(Long.toHexString(object.lIndex)).concat("_");
						ret = ret.concat(entry.lSI.toString()).concat(", ");
						ret = ret.concat(entry.sDefault).concat(", &Obj.");
						ret = ret.concat(object.lEntry.get(0).sVarName).concat(".");//ObjName.VarName
						ret = ret.concat(entry.sVarName);//ObjName.VarName
					}
		    		ret = ret.concat("},\n");
				}
				break;
			case VARIABLE:
				DictEntry entry = object.lEntry.get(0);
				ret = ret.concat("\t{0, ");
				ret = ret.concat(entry.dataType.getTypeEcat()).concat(", ");
				ret = ret.concat(entry.dataType.getBitLen().toString());
				ret = ret.concat(", ").concat(entry.access.getAType());
				ret = ret.concat(", acName").concat(Long.toHexString(object.lIndex));
				ret = ret.concat(", ").concat(entry.sDefault);
				ret = ret.concat(", NULL},\n");
				break;
			case ARRAY:
				DictEntry entry0 = object.lEntry.get(0);
				DictEntry entry1 = object.lEntry.get(1);
				Integer n = Integer.parseInt(entry0.sDefault);
				ret = ret.concat("\t{0, DTYPE_UNSIGNED8, 8, ATYPE_RO, acName");
				ret = ret.concat(Long.toHexString(object.lIndex)).concat("_0,");
				ret = ret.concat(n.toString()).concat(", NULL},\n");
				for (Integer i=1;i<=n;i++)
				{
					ret = ret.concat("\t{");
					ret = ret.concat(i.toString()).concat(", ");
					ret = ret.concat(entry1.dataType.getTypeEcat()).concat(", ");
					ret = ret.concat(entry1.dataType.getBitLen().toString()).concat(", ");
					ret = ret.concat(entry1.access.getAType()).concat(", acName");
					ret = ret.concat(Long.toHexString(object.lIndex)).concat("_");
					ret = ret.concat(i.toString()).concat(", 0, &Obj.");
					ret = ret.concat(object.lEntry.get(1).sVarName).concat("[");//ObjName.VarName
					ret = ret.concat(Integer.toString(i-1)).concat("]},\n");
				}
				break;
			}//switch 
    		ret = ret.concat("};\n");
    	}
    	return ret;
    }
    
    public String generateObjList()
    {
    	String ret = "";
    	for (DictObject object : lObject) {
    		ret = ret+"\t{0x";
    		ret = ret+Long.toHexString(object.lIndex)+", ";
    		ret = ret+object.eObjectCode.getOType()+", ";
    		ret = ret+(object.lEntry.size()-1)+", 0, ";
    		ret = ret+"acName"+Long.toHexString(object.lIndex)+", ";
    		ret = ret+"SDO"+Long.toHexString(object.lIndex)+"}, \n";
    	}
    	return ret;
    }
    
    
}
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	
    	

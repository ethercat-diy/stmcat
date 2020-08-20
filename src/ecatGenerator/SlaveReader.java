package ecatGenerator;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.dom4j.Element;

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
    
    Integer nRxPdoBitSize = 0;
    Integer nTxPdoBitSize = 0;
    
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
	    		if (pattern.matcher(str).matches())
	    			ret = Long.parseLong(str,10);
	    		else if (str.length()<3)
	    			ret = Long.MIN_VALUE;
	    		else if (str.substring(0, 2).compareToIgnoreCase("0x")==0)
	    			ret = Long.parseLong(str.substring(2),16);
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
    			SlaveGenerator.printError(row,"Index should not be empty.");
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
    	ret.put("%(Device Type)", new String("0x")+Long.toHexString(lDeviceProfile));
    	ret.put("%(Vendor ID)", new String("0x")+Long.toHexString(lVenderId));
    	ret.put("%(Product Code)", new String("0x")+Long.toHexString(lProductCode));
    	ret.put("%(Revision Number)", new String("0x")+Long.toHexString(lRevisionNumber));
    	ret.put("%(Serial Number)", new String("0x")+Long.toHexString(lSerialNumber));
    	ret.put("%(Device Name)", sDeviceName);
    	ret.put("%(Hardware Version)", sHWVersion);
    	ret.put("%(Software Version)", sSWVersion);
    	ret.put("%(Group Type)", sGroupType);
    	ret.put("%(Group Name)", sGroupName);
    	ret.put("%(Custom acName)", generateAcString());
    	ret.put("%(Custom SDO)", generateSDO());
    	ret.put("%(nRxPdo)", Integer.toString(objRxPdo.lEntry.size()-1));
    	ret.put("%(nTxPdo)", Integer.toString(objTxPdo.lEntry.size()-1));
    	ret.put("%(nRxPdoByteSize)", Integer.toString((this.nRxPdoBitSize+15)/8));
    	ret.put("%(nTxPdoByteSize)", Integer.toString((this.nTxPdoBitSize+15)/8));
    	ret.put("%(Object List)", generateObjList());
   	return ret;
    }
    
    static private int calLengthPadding(int bitOffset, int bitLen)
    {
    	if(bitLen>=16 || bitOffset%16+bitLen>16)
    		return ((bitOffset+15)/16)*16+bitLen;
    	else
    		return bitOffset+bitLen;
    }
    
    public void generatePdo()
    {
		String sPdoName = new String("TXPDO");
		objTxPdo.lEntry.add(new DictEntry(0L,DataType.BYTE,sPdoName,sPdoName,0L,Access.RO,PdoDir.none,new String("")));
		sPdoName = new String("RXPDO");
		objRxPdo.lEntry.add(new DictEntry(0L,DataType.BYTE,sPdoName,sPdoName,0L,Access.RO,PdoDir.none,new String("")));
		
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
    		String sEntryName;
    		switch (object.eObjectCode)
    		{
    		case RECORD:
	    		for (DictEntry entry: object.lEntry)
	    		{
	    			if(entry.lSI==0)	continue;
	    			sEntryName = sPdoName.concat("_").concat(id.toString());
	    			Long lDefault = (object.lIndex<<16)+(entry.lSI<<8)+(entry.dataType.getBitLen());
	    			DictEntry ePdo = new DictEntry(id++,DataType.UDINT,sEntryName,sEntryName,lDefault,Access.RO,PdoDir.none,new String(""));
	    			ePdo.pointTo(object, entry);
	    			pdoObject.lEntry.add(ePdo);
	    			if (pdoObject == objTxPdo)
	    				this.nTxPdoBitSize = calLengthPadding(this.nTxPdoBitSize, entry.dataType.getBitLen());
	    			else if (pdoObject == objRxPdo)
	    				this.nRxPdoBitSize = calLengthPadding(this.nRxPdoBitSize, entry.dataType.getBitLen());
	    		}
	    		break;
    		case VARIABLE:
    			sEntryName = sPdoName.concat("_").concat(id.toString());
    			DictEntry entry = object.lEntry.get(0);
    			Long lDefault = (object.lIndex<<16)+(entry.lSI<<8)+(entry.dataType.getBitLen());
    			DictEntry ePdo = new DictEntry(id++,DataType.UDINT,sEntryName,sEntryName,lDefault,Access.RO,PdoDir.none,new String(""));
    			ePdo.pointTo(object, entry);
    			pdoObject.lEntry.add(ePdo);
    			if (pdoObject == objTxPdo)
    				this.nTxPdoBitSize = calLengthPadding(this.nTxPdoBitSize, entry.dataType.getBitLen());
    			else if (pdoObject == objRxPdo)
    				this.nRxPdoBitSize = calLengthPadding(this.nRxPdoBitSize, entry.dataType.getBitLen());
    			break;
    		case ARRAY:
    			Long n = object.lEntry.get(0).lDefault;
    			for (Long i=1L;i<=n;i++)
    			{
        			sEntryName = sPdoName.concat("_").concat(id.toString());
    				entry = object.lEntry.get(1);
    				Long lDefault1 = (object.lIndex<<16)+(i<<8)+(entry.dataType.getBitLen());
    				ePdo = new DictEntry(id++,DataType.UDINT,sEntryName,sEntryName,lDefault1,Access.RO,PdoDir.none,new String(""));
        			ePdo.pointTo(object, entry);
        			pdoObject.lEntry.add(ePdo);
	    			if (pdoObject == objTxPdo)
	    				this.nTxPdoBitSize = calLengthPadding(this.nTxPdoBitSize, entry.dataType.getBitLen());
	    			else if (pdoObject == objRxPdo)
	    				this.nRxPdoBitSize = calLengthPadding(this.nRxPdoBitSize, entry.dataType.getBitLen());
    			}
    			break;
    		}
    		if (object.lIndex>=0x6000 && object.lIndex<=0x6999)
    			idTxPdo = id;
    		else if (object.lIndex>=0x7000 && object.lIndex<=0x7999)
    			idRxPdo = id;
    	}
    	objRxPdo.lEntry.get(0).lDefault = objRxPdo.lEntry.size()-1L;
    	objTxPdo.lEntry.get(0).lDefault = objTxPdo.lEntry.size()-1L;
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
				ret = ret.concat(object.lEntry.get(0).lDefault.toString()).concat("];\n");
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
	    		for (Long i = 1L;i<=object.lEntry.get(0).lDefault;i++)
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
						ret = ret.concat(entry.lSI.toString()).concat(", 0x");
						ret = ret.concat(Long.toHexString(entry.lDefault)).concat(", &Obj.");
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
				ret = ret.concat(", 0x").concat(Long.toHexString(entry.lDefault));
				ret = ret.concat(", &Obj.");
				ret = ret.concat(object.lEntry.get(0).sVarName).concat("}\n");
				break;
			case ARRAY:
				DictEntry entry0 = object.lEntry.get(0);
				DictEntry entry1 = object.lEntry.get(1);
				Long n = entry0.lDefault;
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
    		if(object.lIndex<0x6000)
    			continue;
    		ret = ret+"\t{0x";
    		ret = ret+Long.toHexString(object.lIndex)+", ";
    		ret = ret+object.eObjectCode.getOType()+", ";
    		if (object.eObjectCode!=ObjectCode.ARRAY)
    			ret = ret+(object.lEntry.size()-1)+", 0, ";
    		else
    			ret = ret+(object.lEntry.get(0).lDefault)+", 0, ";
    		ret = ret+"acName"+Long.toHexString(object.lIndex)+", ";
    		ret = ret+"SDO"+Long.toHexString(object.lIndex)+"},\n";
    	}
    	return ret;
    }
    
    
    private Element searchElementDataType(Element eDataTypes, String sDataType)
    {
        for (Element e : eDataTypes.elements())
        	if (e.element("Name").getText().compareToIgnoreCase(sDataType)==0)
        		return e;
        return null;
    }
    private void fillDictionaryString(Element eDataTypes, Element eObject, String sContent)
    {
        	Integer iStrLen = sContent.length();
        	String sType = new String("STRING(")+iStrLen.toString()+")";
        	eObject.element("Type").setText(sType);
        	if (searchElementDataType(eDataTypes,sType)==null)
        	{
        		Element eStringType = eDataTypes.addElement("DataType");
        		Element eStringTypeName = eStringType.addElement("Name");
        		eStringTypeName.setText(sType);
        		Element eStringTypeSize = eStringType.addElement("BitSize");
        		eStringTypeSize.setText(Integer.toString(8*iStrLen));
        	}
        	byte[] bytes = {};
			try {
				bytes = sContent.getBytes("ASCII");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	String ordString = "";
        	for (byte b : bytes)
        		ordString = ordString+String.format("%02x", b);
        	eObject.element("Info").element("DefaultData").setText(ordString);
    }
    public String intToHexLsb(Long i)
    {
    	String hexMsb = String.format("%08x", i);
    	String hexLsb = hexMsb.substring(6, 8)+hexMsb.substring(4, 6)+hexMsb.substring(2, 4)+hexMsb.substring(0, 2);
    	return hexLsb;
    }
    public void generateXML(Element root)
    {
		//General vendor infomation
		Element e = root.element("Vendor");
    	e.element("Id").setText(new String("#x")+Long.toHexString(lVenderId));
    	e.element("Name").setText(sVendorName);
    	//General group infomation
    	e = root.element("Descriptions").element("Groups").element("Group");
        e.element("Type").setText(this.sGroupType);
        e.element("Name").setText(this.sGroupName);
    	//General device infomation        
        e = root.element("Descriptions").element("Devices").element("Device").element("Type");
    	e.attribute("ProductCode").setText(new String("#x")+Long.toHexString(this.lProductCode));
    	e.attribute("RevisionNo").setText(new String("#x")+Long.toHexString(this.lRevisionNumber));
        e = root.element("Descriptions").element("Devices").element("Device");
        e.element("Type").setText(this.sGroupName);
    	e.element("Name").setText(this.sDeviceName);
    	e.element("GroupType").setText(this.sGroupType);
    	e.element("Profile").element("ChannelInfo").element("ProfileNo");
    	//General Object Dictionary Existed Items
    	Element eDataTypes = root.element("Descriptions").element("Devices").element("Device").element("Profile").element("Dictionary").element("DataTypes");
    	Element eObjects = root.element("Descriptions").element("Devices").element("Device").element("Profile").element("Dictionary").element("Objects");
    	fillDictionaryString(eDataTypes,eObjects.elements().get(2),this.sDeviceName);
    	fillDictionaryString(eDataTypes,eObjects.elements().get(3),this.sHWVersion);
    	fillDictionaryString(eDataTypes,eObjects.elements().get(4),this.sSWVersion);
    	eObjects.elements().get(5).element("Info").elements().get(1).element("Info").element("DefaultData").setText(intToHexLsb(this.lVenderId));
    	eObjects.elements().get(5).element("Info").elements().get(2).element("Info").element("DefaultData").setText(intToHexLsb(this.lProductCode));
    	eObjects.elements().get(5).element("Info").elements().get(3).element("Info").element("DefaultData").setText(intToHexLsb(this.lRevisionNumber));
    	eObjects.elements().get(5).element("Info").elements().get(4).element("Info").element("DefaultData").setText(intToHexLsb(this.lSerialNumber));
    	//General Application Dictionary
    	for (DictObject object: lObject)
    	{
    		if (object.lIndex<0x6000L)
    			continue;
	    	Element eObject = eObjects.addElement("Object");
	    	Element eObjectIndex = eObject.addElement("Index");
	    	eObjectIndex.setText(new String("#x")+Long.toHexString(object.lIndex));
	    	Element eObjectName = eObject.addElement("Name");
	    	eObjectName.setText(object.lEntry.get(0).sName);
	    	//DictEntry entry;
	    	switch(object.eObjectCode)
	    	{
	    	case RECORD:
	    		eObject.addElement("Type").setText(new String("DT")+String.format("%4x", object.lIndex));
	    		Element eDataType = eDataTypes.addElement("DataType");
	    		eDataType.addElement("Name").setText(eObject.elementText("Type"));
	    		Long bitOffset = 0L;
	    		Element eObjSize = eObject.addElement("BitSize");
	    		Element eDTSize = eDataType.addElement("BitSize");
		    	Element eSubInfo = eObject.addElement("Info");
	    		for (DictEntry entry : object.lEntry)
	    		{
	    			Element eDTSubItem = eDataType.addElement("SubItem");
	    			eDTSubItem.addElement("SubIdx").setText(entry.lSI.toString());
	    			eDTSubItem.addElement("Name").setText(entry.sName);
	    			eDTSubItem.addElement("Type").setText(entry.dataType.name());
	    			Integer lBitLen = entry.dataType.getBitLen();
	    			eDTSubItem.addElement("BitSize").setText(lBitLen.toString());
	    			//16 bit padding
	                if (bitOffset%16L!=0 && lBitLen<16 && (bitOffset%16L+lBitLen>16))
	                    bitOffset = (bitOffset/16L+1L)*16L;
	    			eDTSubItem.addElement("BitOffs").setText(bitOffset.toString());
	    			bitOffset += lBitLen;
	    			if(entry.lSI==0)
		    			eDTSubItem.addElement("Flags").addElement("Access").setText("ro");
	    			else
	    				eDTSubItem.addElement("Flags").addElement("Access").setText(entry.access.name().toLowerCase());
	    			Element eSubItem = eSubInfo.addElement("SubItem");
	    			eSubItem.addElement("Name").setText(entry.sName);
	    			eSubItem.addElement("Info").addElement("DefaultData").setText(intToHexLsb(entry.lDefault));
	    		}
	    		Long iObjSize = (bitOffset+7)/8*8;
	    		eObjSize.setText(iObjSize.toString());
	    		eDTSize.setText(iObjSize.toString());
	    		break;
	    	case ARRAY:
	    		//Generate Array type
	    		eDataType = eDataTypes.addElement("DataType");
	    		eDataType.addElement("Name").setText(new String("DT")+String.format("%4x", object.lIndex)+"ARR");
	    		DictEntry entry0 = object.lEntry.get(0);
	    		DictEntry entry1 = object.lEntry.get(1);
	    		eDataType.addElement("BaseType").setText(entry1.dataType.name());
	    		eDataType.addElement("BitSize").setText(Long.toString(entry1.dataType.getBitLen()*entry0.lDefault));
	    		eDataType.addElement("ArrayInfo").addElement("LBound").setText("1");
	    		eDataType.element("ArrayInfo").addElement("Elements").setText(entry0.lDefault.toString());
	    		//generate data type
	    		eDataType = eDataTypes.addElement("DataType");
	    		eDataType.addElement("Name").setText(new String("DT")+String.format("%4x", object.lIndex));
	    		eDataType.addElement("BitSize").setText(Long.toString(16L+entry1.dataType.getBitLen()*entry0.lDefault));
	    		//generate data type - max sub index
	    		Element eSubItem = eDataType.addElement("SubItem");
	    		eSubItem.addElement("SubIdx").setText("0");
	    		eSubItem.addElement("Name").setText("Max SubIndex");
	    		eSubItem.addElement("Type").setText("USINT");
	    		eSubItem.addElement("BitSize").setText(DataType.USINT.getBitLen().toString());
	    		eSubItem.addElement("BitOffs").setText("0");
	    		eSubItem.addElement("Flags").addElement("Access").setText("ro");
	    		//generate data type - array	Offset set to 16!
	    		eSubItem = eDataType.addElement("SubItem");
	    		eSubItem.addElement("Name").setText("Elements");
	    		eSubItem.addElement("Type").setText(new String("DT")+String.format("%4x", object.lIndex)+"ARR");
	    		eSubItem.addElement("BitSize").setText(Long.toString(entry1.dataType.getBitLen()*entry0.lDefault));
	    		eSubItem.addElement("BitOffs").setText("16");
	    		eSubItem.addElement("Flags").addElement("Access").setText(entry1.access.name().toLowerCase());
	    		//generate object
	    		eObject.addElement("Type").setText(new String("DT")+String.format("%4x", object.lIndex));
	    		eObject.addElement("BitSize").setText(Long.toString(16L+entry1.dataType.getBitLen()*entry0.lDefault));
		    	eSubInfo = eObject.addElement("Info");
	    		eSubItem = eSubInfo.addElement("SubItem");
	    		eSubItem.addElement("Name").setText("Max SubIndex");
	    		eSubItem.addElement("Info").addElement("DefaultData").setText(intToHexLsb(entry0.lDefault));
	    		for (Long idx = 1L; idx<=entry0.lDefault; idx++ )
	    		{
	    			eSubItem = eSubInfo.addElement("SubItem");
	    			eSubItem.addElement("Name").setText(object.lEntry.get(1).sName+idx.toString());
	    			eSubItem.addElement("Info").addElement("DefaultData").setText(intToHexLsb(entry1.lDefault));
	    		}
	    		break;
	    	case VARIABLE:
	    		DictEntry entry = object.lEntry.get(0);
	    		eObject.addElement("Type").setText(entry.dataType.name());
	    		eObject.addElement("BitSize").setText(entry.dataType.getBitLen().toString());
		    	eSubInfo = eObject.addElement("Info");
	    		eSubInfo.addElement("DefaultData").setText(intToHexLsb(entry.lDefault));
	    		break;
	    	}
    	}
    	//General PDO Mapping
    	Element eDT1600 = eDataTypes.elements().get(15);
    	Element eDT1A00 = eDataTypes.elements().get(16);
    	Element eObj1600 = eObjects.elements().get(7);
    	Element eObj1A00 = eObjects.elements().get(8);
    	Element eRxPdo = e = root.element("Descriptions").element("Devices").element("Device").element("RxPdo");
    	Element eTxPdo = e = root.element("Descriptions").element("Devices").element("Device").element("TxPdo");
    	generateXMLPdo(eDT1600,eObj1600,eRxPdo,this.objRxPdo);
    	generateXMLPdo(eDT1A00,eObj1A00,eTxPdo,this.objTxPdo);
    	   	
    }
    private void generateXMLPdo(Element eDataType, Element eObject, Element ePdo, DictObject obj)
    {
		Long lBitSize = 16L+(obj.lEntry.size()-1)*32;
    	eDataType.element("BitSize").setText(lBitSize.toString());
		eObject.element("BitSize").setText(lBitSize.toString());
    	for (DictEntry entry : obj.lEntry)
    	{
    		if (entry.lSI==0)
    			continue;
    		Element eSubItem = eDataType.addElement("SubItem");
    		eSubItem.addElement("SubIdx").setText(entry.lSI.toString());
    		eSubItem.addElement("Name").setText(entry.sName);
    		eSubItem.addElement("Type").setText("UDINT");
    		eSubItem.addElement("BitSize").setText("32");
    		eSubItem.addElement("BitOffs").setText(Long.toString(16L+(entry.lSI-1)*32));
    		eSubItem.addElement("Flags").addElement("Access").setText("ro");
    		
    		eSubItem = eObject.element("Info").addElement("SubItem");
    		eSubItem.addElement("Name").setText(entry.sName);
    		eSubItem.addElement("Info").addElement("DefaultData").setText(intToHexLsb(entry.lDefault));   
    		
    		Element eEntry = ePdo.addElement("Entry");
    		eEntry.addElement("Index").setText(new String("#x")+Long.toHexString(entry.pdoRefObj.lIndex));
    		eEntry.addElement("SubIndex").setText(Long.toString(entry.pdoRefEntry.lSI));
    		eEntry.addElement("BitLen").setText(Long.toString(entry.pdoRefEntry.dataType.getBitLen()));
    		eEntry.addElement("Name").setText(entry.pdoRefEntry.sName);
    		eEntry.addElement("DataType").setText(entry.pdoRefEntry.dataType.name());
    	}
    }
    
}
    	

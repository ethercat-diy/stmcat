package ecatGenerator;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;

public class DictEntry {
	public Long lSI;
	public DataType dataType;
	public String sName;
	public String sVarName;
	public String sDefault;
	public Access access;
	public PdoDir pdoDir;
	public String sDescription;
	
	DictEntry(Long lSI_, DataType dataType_, String sName_, String sVarName_, String sDefault_, Access access_, PdoDir pdoDir_, String sDesc_)
	{
		lSI = lSI_;
		dataType = dataType_;
		sName = sName_;
		sVarName = sVarName_;
		sDefault = sDefault_;
		access = access_;
		pdoDir = pdoDir_;
		sDescription = sDesc_;
	}
	
    
    DictEntry(XSSFRow row)
    {
    	XSSFCell cell;
		cell = row.getCell(3);
		lSI = SlaveReader.getLongCellValue(cell);
		try {
			cell = row.getCell(4);
			dataType = DataType.parse(SlaveReader.getStringCellValue(cell));
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			SlaveGenerator.printError(row, "Unrecognized data type.");
			dataType = DataType.BYTE;
		}
		cell = row.getCell(5);
		sName = SlaveReader.getStringCellValue(cell);
		sVarName = sName.replaceAll("\\s*", "");
		cell = row.getCell(6);
		sDefault = SlaveReader.getStringCellValue(cell);
		try	{
			cell = row.getCell(11);
			access = Access.parse(SlaveReader.getStringCellValue(cell));
		}
		catch(NumberFormatException e)
		{
			e.printStackTrace();
			SlaveGenerator.printError(row, "Access code should be either RO or RW.");
			access = Access.RO;
		}
		try	{
			cell = row.getCell(12);
			pdoDir = PdoDir.parse(SlaveReader.getStringCellValue(cell));
		}
		catch(NumberFormatException e)
		{
			SlaveGenerator.printError(row, "PDO direction should be empty, rx or tx.");
			e.printStackTrace();
			pdoDir = PdoDir.none;
		}

		cell = row.getCell(14);
		sDescription = SlaveReader.getStringCellValue(cell);
    	
		//Check Default
		if (sDefault.isEmpty())
			sDefault = "0";
    }
}

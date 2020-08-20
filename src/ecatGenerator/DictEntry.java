package ecatGenerator;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;

public class DictEntry {
	public Long lSI;
	public DataType dataType;
	public String sName;
	public String sVarName;
	public Long lDefault;
	public Access access;
	public PdoDir pdoDir;
	public String sDescription;
	public DictObject pdoRefObj;//Only used by 0x1A00 and 0x1600
	public DictEntry pdoRefEntry;//Only used by 0x1A00 and 0x1600
	
	DictEntry(Long lSI_, DataType dataType_, String sName_, String sVarName_, Long lDefault_, Access access_, PdoDir pdoDir_, String sDesc_)
	{
		lSI = lSI_;
		dataType = dataType_;
		sName = sName_;
		sVarName = sVarName_;
		lDefault = lDefault_;
		access = access_;
		pdoDir = pdoDir_;
		sDescription = sDesc_;
		pdoRefObj = null;
		pdoRefEntry = null;
	}
	
	public void pointTo(DictObject obj_, DictEntry entry_)
	{
		pdoRefObj = obj_;
		pdoRefEntry = entry_;
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
		if (cell==null)
			lDefault = 0L;
		else
			lDefault = SlaveReader.getLongCellValue(cell);
		if (lDefault == Long.MIN_VALUE)
		{
			lDefault = 0L;
			//SlaveGenerator.consoleStream.println("Warning: Only integer 'Default' value is accepted, or 0 would be filled.");
		}
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
    	
    }
}

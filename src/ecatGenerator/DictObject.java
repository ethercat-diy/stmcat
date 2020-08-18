package ecatGenerator;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;


public class DictObject {
	public Long lIndex;
	public ObjectCode eObjectCode;
	public List<DictEntry> lEntry;
	
	DictObject(Long lIndex_, ObjectCode eObjectCode_, DictEntry e)
	{
		lIndex = lIndex_;
		eObjectCode = eObjectCode_;
		lEntry = new ArrayList<DictEntry>();
		if (e!=null)
			lEntry.add(e);
	}
	DictObject(XSSFRow row)	//Extract a new object
	{
    	XSSFCell cell;
		cell = row.getCell(1);
		lIndex = SlaveReader.getLongCellValue(cell);
		cell = row.getCell(2);
		try	{
			eObjectCode = ObjectCode.parse(SlaveReader.getStringCellValue(cell));
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			SlaveGenerator.consoleStream.println(new String("Error: Row").concat(Integer.toString(row.getRowNum()))
					.concat(". Unrecognized object code."));
		}
		
		lEntry = new ArrayList<DictEntry>();
		DictEntry entry = new DictEntry(row);
		//SI set to 0
		entry.lSI = 0L;
		//check "default" when ARRAY[0]
		if (eObjectCode==ObjectCode.ARRAY)
		{
			try{
			     int i = Integer.parseInt(entry.sDefault);
			     if (i<0)
			     {
						SlaveGenerator.printError(row, "Wrong array length.");
						entry.sDefault = "1";
			     }
			}
			catch(NumberFormatException e){
				SlaveGenerator.printError(row, "Wrong array length.");
			    entry.sDefault = "1";
			}
		}
		lEntry.add(entry);
	}
	
	void append(XSSFRow row)
	{
		DictEntry entry = new DictEntry(row);
		//Is SubIndex successive?
		Long lSI_ = lEntry.get(lEntry.size()-1).lSI;
		if (entry.lSI!=lSI_+1)
		{
			SlaveGenerator.printError(row, "SubIndex is not continuous.");
			entry.lSI = lSI_+1;
		}
		lEntry.add(entry);
	}
	
}

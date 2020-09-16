package ecatGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.osgi.framework.Bundle;

import stmcat.Activator;

public class SlaveGenerator {
	//Inputs
	InputStream inputStream;
	IProject prj;
	Shell shell;
	
	//For console output
	MessageConsole console = null;
	public static MessageConsoleStream consoleStream = null;
	IConsoleManager consoleManager = null;
	final String CONSOLE_NAME = "STM CAT Console";
	
	private void initConsole() {
		consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		IConsole[] consoles = consoleManager.getConsoles();
		if (consoles.length>0)
		{
			for (IConsole iconsole: consoles)
				if (iconsole.getName()==CONSOLE_NAME)
				{
					console = (MessageConsole)iconsole;
					break;
				}
		}
		if (console == null)
		{
			console = new MessageConsole(CONSOLE_NAME,null);
			consoleManager.addConsoles(new IConsole[] {console} );
		}
		consoleStream = console.newMessageStream();
		consoleManager.showConsoleView(console);
	}
	
	public SlaveGenerator(InputStream inputStream, IProject prj, Shell shell)
	{
		this.inputStream = inputStream;
		this.prj = prj;
		this.shell = shell;
		initConsole();
	}
	
	public static void printError(XSSFRow row, String message)
	{
		consoleStream.println(new String("Error: Row").concat(Integer.toString(row.getRowNum()))
				.concat(". ").concat(message));
	}
	
	private String readTemplate(String filename)
	{
		String str = "";
		consoleStream.println("Finding plugin files...");
		Bundle bundle = Activator.getDefault().getBundle();
		URL url = bundle.getResource(filename);
		consoleStream.println("Reading plugin files...");
		try {
			consoleStream.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			InputStream isTemplate = FileLocator.toFileURL(url).openStream();
		    int length = isTemplate.available();
		    byte bytes[] = new byte[length];
		    isTemplate.read(bytes);
		    isTemplate.close();
		    str =new String(bytes);
			consoleStream.println("Plugin files has been read.");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			consoleStream.println("Could not locate plugin files.");
			e1.printStackTrace();
		}
		return str;
	}
	
	private IFile writeFile(IFolder folder, String filename, String content)
	{
        
        IFile ifile = folder.getFile(filename);

		try {
			//Write file system
			File fOutput=new File(ifile.getLocation().toOSString());
			byte bytes[]=new byte[content.length()];   
	        bytes=content.getBytes();  
	        int b=bytes.length;   //是字节的长度，不是字符串的长度
	        FileOutputStream fos;
			fos = new FileOutputStream(fOutput);
			fos.write(bytes,0,b); 
            fos.close();
            consoleStream.println(fOutput.getAbsolutePath());	
            ifile.createLink(fOutput.toURI(), 1, null);

            //Create project file
//            InputStream fileStream = new FileInputStream(fOutput);
//            ifile.create(fileStream, true, null);
//		} catch (CoreException | IOException e) {
		} catch ( IOException | CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			consoleStream.println("Could not write source files.");
		}
		return ifile;
	}
	
	private IFile copyFile(IFolder folder, String filenameDest, String filenameSrc)
	{
        IFile ifile = folder.getFile(filenameDest);
		Bundle bundle = Activator.getDefault().getBundle();
		URL url = bundle.getResource(filenameSrc);
		try {
			InputStream isSrc = FileLocator.toFileURL(url).openStream();
	        ifile.create(isSrc, true, null);
	        isSrc.close();
		} catch (IOException | CoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return ifile;
	}

	
	private IFolder initOutput(IProject prj)
	{
		consoleStream.println("Generating soes folder...");

		IFolder folder = prj.getFolder("/soes");
		try {
			if (folder.exists())
				folder.delete(true, null);
			folder = prj.getFolder("/soes");
			folder.create(true, true, null);
			consoleStream.println("SOES folder has be created.");
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			consoleStream.println("SOES folder cannot be created.");
		}
		return folder;
	}
    public static String format(String input, Map<String, String> map) {
        // 遍历map,用value替换掉key
        for (Map.Entry<String, String> entry : map.entrySet()) {
            input = input.replace(entry.getKey(), entry.getValue());
        }
        return input;
    }
	
	public int generate()
	{
		//Check whether input is valid
		try {	if (inputStream.available()==0)			{
				consoleStream.println("No valid input stream...");
				return 1;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		
		consoleStream.println("Parsing xlsx file...");
		try {//Read xlsx file
			XSSFWorkbook myWorkBook = new XSSFWorkbook(inputStream);
			XSSFSheet mySheet = myWorkBook.getSheetAt(0);
			SlaveReader slaveReader = new SlaveReader(mySheet);
			myWorkBook.close();

			consoleStream.println("Generating C files...");
			IFolder folder = initOutput(prj);
			//Generate slave_objectlist.c
			String str = readTemplate("/templates/slave_objectlist.tmp");
			HashMap<String,String> map = slaveReader.generateMapC();
			str = format(str,map);
			writeFile(folder, "slave_objectlist.c", str);

			//Generate utypes.h
			str = readTemplate("/templates/utypes.tmp");
			str = str.replace("%(Objects)", slaveReader.generateUserObjects());
			writeFile(folder, "utypes.h", str);
			
			//Generate options.h
			str = readTemplate("/templates/options.tmp");
			str = format(str,map);
			writeFile(folder, "options.h", str);
			
			
			
			//Copy all the other files
			copyFile(folder,"cc.h", "/templates/cc.h");
			copyFile(folder,"ecat_slv.c", "/templates/ecat_slv.c");
			copyFile(folder,"ecat_slv.h", "/templates/ecat_slv.h");
			copyFile(folder,"ecat_task.c", "/templates/ecat_task.c");
			copyFile(folder,"esc_coe.c", "/templates/esc_coe.c");
			copyFile(folder,"esc_coe.h", "/templates/esc_coe.h");
			copyFile(folder,"esc_eep.c", "/templates/esc_eep.c");
			copyFile(folder,"esc_eep.h", "/templates/esc_eep.h");
			copyFile(folder,"esc_eoe.c", "/templates/esc_eoe.c");
			copyFile(folder,"esc_eoe.h", "/templates/esc_eoe.h");
			copyFile(folder,"esc_foe.c", "/templates/esc_foe.c");
			copyFile(folder,"esc_foe.h", "/templates/esc_foe.h");
			copyFile(folder,"esc_hw.c", "/templates/esc_hw.c");
			copyFile(folder,"esc.c", "/templates/esc.c");
			copyFile(folder,"esc.h", "/templates/esc.h");
//			copyFile(folder,"options.h", "/templates/options.h");
			
			//Generate XML file
			SlaveGenerator.consoleStream.println("Generating ESI XML file...");
			SAXReader sax = new SAXReader();
			Bundle bundle = Activator.getDefault().getBundle();
			URL url = bundle.getResource("/templates/SSC-Template.xml");
			InputStream isTemplate = FileLocator.toFileURL(url).openStream();
			Document doc = sax.read(isTemplate);
			Element root = doc.getRootElement();
			slaveReader.generateXML(root);
			IFile ifile = folder.getFile(slaveReader.sDeviceName.replaceAll("\\s*", "")+".xml");
			File fOutput=new File(ifile.getLocation().toOSString());
			OutputStream os = new FileOutputStream(fOutput);
			OutputFormat format = OutputFormat.createPrettyPrint();
			format.setEncoding("utf-8");
			XMLWriter xw = new XMLWriter(os,format);
			xw.write(doc);
			xw.flush();
			xw.close();
			ifile.createLink(fOutput.toURI(), 1, null);
		} catch (IOException | DocumentException | CoreException e) {
			e.printStackTrace();
		}
		
		SlaveGenerator.consoleStream.println("Slave device generation finished.");
		
//		MessageBox msg = new MessageBox(parentShell);
//		msg.setMessage(str);
//		msg.open();
		
		return 0;
	}
	
	

}

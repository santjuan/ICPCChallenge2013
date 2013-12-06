import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JOptionPane;


public class GameVisualizer 
{
	static final boolean debug = true;
	
	public static void main(String[] args) throws IOException, InterruptedException
	{
		ArrayList <String> estrategias = new ArrayList <String> ();
		for(File f : new File("src/").listFiles())
		{
			if(f.getName().contains("GameVisualizer"))
				continue;
			if(f.getName().contains("GameTester"))
				continue;
			@SuppressWarnings("resource")
			Scanner sc = new Scanner(f);
			boolean ok = false;
			boolean ok2 = false;
			while(sc.hasNextLine())
			{
				String linea = sc.nextLine();
				if(linea.contains("main(String[] args)"))
					ok = true;
				if(linea.contains("extends Game.Strategy"))
					ok2 = true;
			}
			if(ok && ok2)
				estrategias.add(f.getName().replace(".java", ""));
		}
		String[] options = estrategias.toArray(new String[0]);
		int a = JOptionPane.showOptionDialog(null, "Escoja la estrategia A", "Estrategia", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		int b = JOptionPane.showOptionDialog(null, "Escoja la estrategia B", "Estrategia", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		ArrayList <String> mapasA = new ArrayList <String> ();
		for(File f : new File("maps/").listFiles())
			mapasA.add(f.getName().replace(".txt", ""));
		String[] mapas = mapasA.toArray(new String[0]);
		int mapa = JOptionPane.showOptionDialog(null, "Escoja el mapa", "Mapa", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, mapas, mapas[0]);
		int duration = 700;
		boolean sync = true;//options[a].contains("Debug") || options[b].contains("Debug");
		System.out.println("cmd /c start cmd.exe /K \"" + "java -jar coderunner.jar -player " +  (sync ? "sync" : "") + "java -cp bin " + options[a] + "  -player " + (sync ? "sync" : "") + "java -cp bin " + options[b] + " -map maps/" + mapas[mapa] + ".txt\" && exit");
		Runtime.getRuntime().exec("cmd /c start cmd.exe /K \"" + "java -jar coderunner.jar -player " +  (sync ? "sync" : "") + "java -cp bin " + options[a] + "  -player " + (sync ? "sync" : "") + "java -cp bin " + options[b] + " -map maps/" + mapas[mapa] + ".txt -duration " + duration + "\" && exit");
		System.exit(0);
	}
}

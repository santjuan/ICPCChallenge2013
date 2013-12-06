import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JOptionPane;


public class GameVisualizer2 
{
	
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
		int id = Integer.parseInt(JOptionPane.showInputDialog("Ingrese el escogido"));
		String[] options = estrategias.toArray(new String[0]);
		int a = JOptionPane.showOptionDialog(null, "Escoja la estrategia A", "Estrategia", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		int b = JOptionPane.showOptionDialog(null, "Escoja la estrategia B", "Estrategia", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		ArrayList <String> mapasA = new ArrayList <String> ();
		for(File f : new File("maps/").listFiles())
			mapasA.add(f.getName().replace(".txt", ""));
		String[] mapas = mapasA.toArray(new String[0]);
		int mapa = JOptionPane.showOptionDialog(null, "Escoja el mapa", "Mapa", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, mapas, mapas[0]);
		boolean sync = false;//options[a].contains("Debug") || options[b].contains("Debug");
		String rutaA = id == 0 ? "\"C:\\Users\\santjuan\\workspace\\ICPCChallengeOfficial\\bin\" TspMainTest" : "bin " + options[a];
		String rutaB = id == 1 ? "\"C:\\Users\\santjuan\\workspace\\ICPCChallengeOfficial\\bin\" TspMainTest" : "bin " + options[b];
		System.err.println(rutaA);
		System.err.println(rutaB);
		System.out.println("cmd /c start cmd.exe /K \"" + "java -jar coderunner.jar -player " +  (sync ? "sync" : "") + "java -cp " + rutaA + "  -player " + (sync ? "sync" : "") + "java -cp " + rutaB + " -map maps/" + mapas[mapa] + ".txt\"");
		final Process p = Runtime.getRuntime().exec("cmd /c start cmd.exe /K \"" + "java -jar coderunner.jar -player " +  (sync ? "sync" : "") + "java -cp " + rutaA + "  -player " + (sync ? "sync" : "") + "java -cp " + rutaB + " -map maps/" + mapas[mapa] + ".txt\"");
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			
			@Override
			public void run() {
				for(int i = 0; i < 100; i++)
					p.destroy();
			}
		}));
	}

}

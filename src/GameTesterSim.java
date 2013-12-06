import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import javax.swing.JOptionPane;

public class GameTesterSim
{	
	static final boolean debug = true;
	
	public static void main(String[] args) throws IOException, InterruptedException
	{
		System.out.println("Tester output");
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
			mapasA.add(f.getName());
		String[] mapas = mapasA.toArray(new String[0]);
		int duration = 700;
		boolean sync = RealtimeState.IN_SYNC;//options[a].contains("Debug") || options[b].contains("Debug");
		int totalR = 0;
		int victoriasA = 0;
		int victoriasB = 0;
		System.out.println(options[a] + " vs " + options[b]);
		for(String mapa : mapas)
		{
			if(mapa.startsWith("mapg.txt"))
				break;
			System.out.println(mapa);
			int[] vals = getScore(options[a], options[b], mapa, sync, duration);
			System.out.println(Arrays.toString(vals));
			int tot = vals[0] - vals[1];
			if(vals[0] > vals[1])
				victoriasA++;
			else if(vals[0] < vals[1])
				victoriasB++;
			vals = getScore(options[b], options[a], mapa, sync, duration);
			int tmp = vals[0];
			vals[0] = vals[1];
			vals[1] = tmp;
			tot += vals[0] - vals[1];
			if(vals[0] > vals[1])
				victoriasA++;
			else if(vals[0] < vals[1])
				victoriasB++;
			System.out.println(Arrays.toString(vals));
			System.out.println(tot);
			totalR += tot;
		}
		System.out.println(victoriasA + " " + victoriasB + " " + totalR);
		System.exit(0);
	}
	
	static int[] getScore(String strategy1, String strategy2, String map, boolean sync, int duration) throws IOException, InterruptedException
	{
		try
		{
			Game.Strategy a = (Game.Strategy) Class.forName(strategy1).newInstance();
			Game.Strategy b = (Game.Strategy) Class.forName(strategy2).newInstance();
			return SimulatedRealtimeState.simulate(a, b, false, map, duration);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}

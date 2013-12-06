import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class SimulatedRealtimeState
{
	Simulator simulator;
	Game.InitialState initial;
	
	@SuppressWarnings("resource")
	public SimulatedRealtimeState(String map, int turnCount) 
	{
		Scanner sc;
		try 
		{
			sc = new Scanner(new File("maps/" + map));
		}
		catch (Exception e) 
		{
			throw new RuntimeException(e);
		}
		char[][] originalMap = new char[16][];
		for(int i = 0; i < 16; i++)
			originalMap[i] = sc.next().toCharArray();
		int redI = 0, redJ = 0, blueI = 0, blueJ = 0;
		for(int i = 0; i < 16; i++)
			for(int j = 0; j < 25; j++)
			{
				if(originalMap[i][j] == 'R')
				{
					redI = i;
					redJ = j;
					originalMap[i][j] = '.';
				}
				if(originalMap[i][j] == 'B')
				{
					blueI = i;
					blueJ = j;
					originalMap[i][j] = '.';
				}
			}
		int nEnemies = sc.nextInt();
		char[][] enemyPrograms = new char[nEnemies << 1][];
		Game.Position[] enemiesStarting = new Game.Position[nEnemies << 1];
		for(int i = 0; i < nEnemies; i++)
		{
			enemiesStarting[i] = new Game.Position(sc.nextInt(), sc.nextInt());
			enemiesStarting[i + nEnemies] = new Game.Position(enemiesStarting[i].i, 24 - enemiesStarting[i].j);
			enemyPrograms[i] = sc.next().toCharArray();
			StringBuilder sb = new StringBuilder();
			for(char c : enemyPrograms[i])
				if(c == 'L')
					sb.append('R');
				else if(c == 'R')
					sb.append('L');
				else
					sb.append(c);
			enemyPrograms[i + nEnemies] = sb.toString().toCharArray();
		}
		initial = new Game.InitialState();
		initial.turnCount = turnCount;
		initial.startingBoard = originalMap;
		initial.redStarting = new Game.Position(redI, redJ);
		initial.blueStarting = new Game.Position(blueI, blueJ);
		initial.enemiesProgram = enemyPrograms;
		initial.enemiesStarting = enemiesStarting;
		simulator = new Simulator(initial, new OriginalMap(originalMap, true));
	}
	
	static int[] simulate(Game.Strategy a, Game.Strategy b, boolean printBoard, String map, int duration)
	{
		try
		{
			System.setErr(new PrintStream("tmp.txt"));
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		SimulatedRealtimeState game = new SimulatedRealtimeState(map, duration);
		a.init(game.simulator, game.initial, true);
		b.init(game.simulator, game.initial, false);
		char[][] board = game.simulator.getOriginalMap().map.clone();
		for(int i = 0; i < 16; i++)
			board[i] = board[i].clone();
		while(game.simulator.currentTurn < game.simulator.turnCount)
		{
			if(printBoard)
			{
				try
				{
					Thread.sleep(100);
				}
				catch (InterruptedException e)
				{
				}
				game.simulator.fillMap(board, true, true, true);
				putPlayer(game.simulator, board, true);
				putPlayer(game.simulator, board, false);
				for(int i = 0; i < 35; i++) System.out.println();
				System.out.println("Turn #" + game.simulator.currentTurn + ":");
				for(char[] c : board) System.out.println(new String(c));
				System.out.println(Arrays.toString(new int[] {game.simulator.getPlayer(true).score, game.simulator.getPlayer(false).score}));
				removePlayer(game.simulator, board, true);
				removePlayer(game.simulator, board, false);
				game.simulator.clearMap(board, true, true, true);
			}
			game.simulator.simulateTurn(new int[]{a.getPlay(game.simulator), b.getPlay(game.simulator)});
		}
		return new int[] {game.simulator.getPlayer(true).score, game.simulator.getPlayer(false).score};
	}

	private static void removePlayer(Simulator simulator, char[][] board, boolean red)
	{
		Game.GamePlayer player = simulator.getPlayer(red);
		if(player.i < 0) return;
		board[player.i][player.j] = simulator.getOriginalMap().map[player.i][player.j];
	}

	static void putPlayer(Simulator simulator, char[][] board, boolean red) 
	{
		Game.GamePlayer player = simulator.getPlayer(red);
		if(player.i < 0) return;
		if(board[player.i][player.j] == 'Z')
			board[player.i][player.j] = 'D';
		else
			board[player.i][player.j] = red ? 'R' : 'B';
	}
	
	public static void main(String[] args)
	{
		Random r = new Random();
		ArrayList <String> mapasA = new ArrayList <String> ();
		for(File f : new File("maps/").listFiles())
			mapasA.add(f.getName());
		int cuenta = 0;
		while(true)
		{
			String mapa = mapasA.get(r.nextInt(mapasA.size()));
			System.out.println("Simulation number " + cuenta + " " + mapa);
			boolean red = r.nextBoolean();
			MLTrainerTest mlTest = new MLTrainerTest(args.length == 0 ? "t-set.txt" : args[0]);
			Game.Strategy a = red ? mlTest : new Tsp3Main();
			Game.Strategy b = red ? new Tsp3Main() : mlTest;
			System.out.println(a.getClass().getName() + " " + b.getClass().getName());
			int[] ans = simulate(a, b, cuenta < 2, mapa, 700);
			System.out.println(Arrays.toString(ans));
			mlTest.writeAll();
			cuenta++;
		}
	}
}
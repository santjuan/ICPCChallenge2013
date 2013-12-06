import java.util.ArrayList;
import java.util.Arrays;


public class ZombieLadders 
{
	static final int POSITION_MASK = (1 << 16) - 1;
	static final int DIRECTION_MASK = ~POSITION_MASK;
	static final int UP = 1;
	static final int DOWN = 2;
	static final int LEFT = 4;
	static final int RIGHT = 8;
	static final int DIG_LEFT = 16;
	static final int DIG_RIGHT = 32;
	static final int NONE = 64;
	static final int SUICIDE = 128;
	final char[][] map;
	final double[][] zombieLadder;
	static final int[][] diffs = new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
	static final int[][] ladderDiffs2 = new int[][]{{1, 0, DOWN}, {-1, 0, UP}};
	
	ZombieLadders(OriginalMap m, Game.GameEnemy[] enemies)
	{
		map = m.map;
		zombieLadder = new double[16][25];
		for(Game.GameEnemy e : enemies)
		{
			if(e instanceof Simulator.Enemy)
			{

				UtilClasses.FastStack stack = ((Simulator.Enemy) e).stackOriginal;
				if(stack != null)
				{
					stack = stack.clonar();
					int posI = e.startI;
					int posJ = e.startJ;
					int iteraciones = 0;
					double tamStack = stack.size;
					if(stack.size == 0)
						tamStack = 1;
					do
					{
						if(stack.size == 0)
							break;
						zombieLadder[posI][posJ] += map[posI][posJ] == 'H' ? 2.0 / tamStack : 1.0 / tamStack;
						int move = stack.pop();
						posI += diffs[move][0];
						posJ += diffs[move][1];
					}
					while((!(posI == e.startI && posJ == e.startJ)) && iteraciones++ < 200);
				}
			}
		}
	}
	boolean isValid(int i, int j)
	{
		return i < 16 && i >= 0 && j < 25 && j >= 0 && map[i][j] != '=';
	}
	
	boolean isFalling(int i, int j)
	{
		return i + 1 < 16 && map[i][j] == '.' && map[i + 1][j] == '.';
	}
	
	int[] generateNeighborsDigging2(int from, int dig, int floor, ArrayList <Integer> all)
	{
		int i = from / 25;
		int j = from % 25;
		all.clear();
		if(map[i][j] == '=' && floor == 0)
			return new int[]{};
		if(isFalling(i, j) || (floor == 4 && isValid(i + 1, j) && map[i + 1][j] == '.'))
			return new int[]{convert2(i + 1, j, dig == 0 ? 0 : dig - 1, 0, DOWN)};
		if(floor == 2)
			if(i == 15)
				return new int[]{};
			else
				return new int[]{convert2(i + 1, j, dig == 0 ? 0 : dig - 1, 4, DOWN)};
		if(dig != 0)
			all.add(convert2(i, j, dig - 1, floor, NONE));
		if(dig == 0 && i < 15 && map[i + 1][j] == '=')
		{
			if(j < 24 && map[i][j + 1] == '.' && map[i + 1][j + 1] == '=')
				all.add(convert2(i, j, 10, 3, DIG_RIGHT));
			if(j >= 1 && map[i][j - 1] == '.' && map[i + 1][j - 1] == '=')
				all.add(convert2(i, j, 10, 1, DIG_LEFT));
		}
		if(isValid(i, j - 1))
			all.add(convert2(i, j - 1,  dig == 0 ? 0 : dig - 1, floor == 1 ? 2 : 0, LEFT));
		if(isValid(i, j + 1))
			all.add(convert2(i, j + 1,  dig == 0 ? 0 : dig - 1, floor == 3 ? 2 : 0, RIGHT));
		if(map[i][j] == 'H')
		{
			for(int[] d : ladderDiffs2)
				if(isValid(i + d[0], j + d[1]))
					all.add(convert2(i + d[0], j + d[1], dig == 0 ? 0 : dig - 1, 0, d[2]));
		}
		if(i < 15 && map[i + 1][j] == 'H')
		{
			if(isValid(i + 1, j))
				all.add(convert2(i + 1, j, dig == 0 ? 0 : dig - 1, 0, DOWN));
		}
		int[] neigh = new int[all.size()];
		int indice = 0;
		for(int n : all) neigh[indice++] = n;
		return neigh;
	}
	
	int convert2(int i, int j, int dig, int floor, int dir)
	{
		return (i * 25 + j) + (dig << 9) + (floor << 15) + (dir << 20);
	}
	
	
	int[] ansTmp2;
	double[] ansTmp3;
	static final int MASK_POS_FROM = (1 << 20) - 1;
	static final int MASK_DIR = ~MASK_POS_FROM;
	ArrayList <Integer> allTmp = new ArrayList <Integer> ();
	UtilClasses.FastArrayDeque colaTmp;
	double[][] shortestDiggingPath;
	
	synchronized double[] fillDigging(int from, int dig, int floor)
	{
		int[] ans = ansTmp2;
		double[] ansLadders = ansTmp3;
		Arrays.fill(ans, Integer.MAX_VALUE);
		Arrays.fill(ansLadders, 0);
		UtilClasses.FastArrayDeque cola = colaTmp;
		cola.clear();
		int indiceInicio = convert2(from / 25, from % 25, dig, floor, 0);
		ans[indiceInicio] = 0;
		ansLadders[indiceInicio] = zombieLadder[from / 25][from % 25];
		cola.clear();
		cola.add(indiceInicio);
		while(!cola.isEmpty())
		{
			int entrada = cola.poll();
			int posM = entrada & 511;
			int d = (entrada >> 9) & 15;
			int p = (entrada >> 15) & 7;
			int actualV = ans[entrada] & POSITION_MASK;
			int actualDir = ans[entrada] & DIRECTION_MASK;
			for(int vecino : generateNeighborsDigging2(posM, d, p, allTmp))
			{
				int vecinoR = vecino & MASK_POS_FROM;
				int vecinoV = vecinoR & 511;
				if(actualV + 1 == (ans[vecinoR] & POSITION_MASK))
				{
					ans[vecinoR] |= actualDir;
					ansLadders[vecinoR] = Math.max(ansLadders[vecinoR], ansLadders[entrada] + (zombieLadder[vecinoV / 25][vecinoR % 25]));
				}
				else if(ans[vecinoR] == Integer.MAX_VALUE)
				{
					int dirV = entrada == indiceInicio ? (((vecino & MASK_DIR) >>> 20) << 16) : actualDir;
					ans[vecinoR] = (actualV + 1) | dirV;
					ansLadders[vecinoR] = ansLadders[entrada] + (zombieLadder[vecinoV / 25][vecinoR % 25]);
					cola.add(vecinoR);
				}
			}
		}
		int[] respuesta = new int[400];
		double[] respuestaLadders = new double[400];
		Arrays.fill(respuesta, Integer.MAX_VALUE);
		for(int i = 0; i < 16; i++)
			for(int j = 0; j < 25; j++)
				for(int d = 0; d <= 10; d++)
					for(int f = 0; f < 5; f++)
					{
						int r = ans[convert2(i, j, d, f, 0)] & POSITION_MASK;
						if(r < (respuesta[i * 25 + j] & POSITION_MASK))
						{
							respuesta[i * 25 + j] = ans[convert2(i, j, d, f, 0)];
							respuestaLadders[i * 25 + j] = ansLadders[convert2(i, j, d, f, 0)];
						}
					}
		return respuestaLadders;
	}
	
	int getFloor(int i, int j, char[][] mapWithZombiesAndHoles)
	{
		if(mapWithZombiesAndHoles[i][j] == '-') return 4;
		else if(i < 15 && j >= 1 && mapWithZombiesAndHoles[i + 1][j - 1] == '-') return 1;
		else if(i < 15 && mapWithZombiesAndHoles[i + 1][j] == '-') return 2;
		else if(i < 15 && j < 24 && mapWithZombiesAndHoles[i + 1][j + 1] == '-') return 3;
		else return 0;
	}
	
	synchronized double[] getDiggingFast(int i, int j, int dig, char[][] mapWithZombiesAndHoles)
	{
		int floor = getFloor(i, j, mapWithZombiesAndHoles);
		int indice = convert2(i, j, dig, floor, 0);
		if(ansTmp2 == null)
		{
			ansTmp2 = new int[1 << 18];
			ansTmp3 = new double[1 << 18];
			colaTmp = new UtilClasses.FastArrayDeque(1 << 18);
			shortestDiggingPath = new double[1 << 18][];
		}
		if(shortestDiggingPath[indice] == null) shortestDiggingPath[indice] = fillDigging(i * 25 + j, dig, floor);
		return shortestDiggingPath[indice];	
	}
}
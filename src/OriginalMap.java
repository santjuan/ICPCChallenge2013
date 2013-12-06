import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;

public class OriginalMap 
{
	static class Entry implements Comparable <Entry>
	{
		int pos;
		int dig;
		int weight;
		
		public Entry(int p, int d, int w)
		{
			pos = p;
			dig = d;
			weight = w;
		}

		@Override
		public int compareTo(Entry o) 
		{
			return weight - o.weight;
		}
	}
	
	char[][] map;
	int[][] shortestReversiblePath = new int[400][];
	int[][] shortestFallingPath = new int[400][];
	int[][] neighbors = new int[400][];
	static final int UP = 1;
	static final int DOWN = 2;
	static final int LEFT = 4;
	static final int RIGHT = 8;
	static final int DIG_LEFT = 16;
	static final int DIG_RIGHT = 32;
	static final int NONE = 64;
	static final int SUICIDE = 128;
	static final int POSITION_MASK = (1 << 16) - 1;
	static final int DIRECTION_MASK = ~POSITION_MASK;
	static final int[][] ladderDiffs2 = new int[][]{{1, 0, DOWN}, {-1, 0, UP}};
	static final int[][] ladderDiffs = new int[][]{{1, 0, DOWN}, {-1, 0, UP}, {0, 1, RIGHT}, {0, -1, LEFT}};
	static final int[][] normalDiffs = new int[][]{{0, 1, RIGHT}, {0, -1, LEFT}};
	static final int[][] normalLadderDiffs = new int[][]{{1, 0, DOWN}, {0, 1, RIGHT}, {0, -1, LEFT}};
	
	public OriginalMap(char[][] m, boolean fillEarly) 
	{
		map = m.clone();
		for(int i = 0; i < map.length; i++)
		{
			map[i] = map[i].clone();
			for(int j = 0; j < map[i].length; j++)
				if(map[i][j] != '.' && map[i][j] != 'H' && map[i][j] != '=')
					map[i][j] = '.';
		}
		ArrayList <Integer> all = new ArrayList <Integer> ();
		for(int i = 0; i < 400; i++) generateNeighbors(i, all);
		if(fillEarly)
		{
			UtilClasses.FastArrayDeque deque = new UtilClasses.FastArrayDeque(400);
			for(int i = 0; i < 400; i++) shortestReversiblePath[i] = fill(i, true, deque);
			for(int i = 0; i < 400; i++) shortestFallingPath[i] = fill(i, false, deque);
		}
	}
	
	int[] fill(int from, boolean reversible)
	{
		int[] ans = new int[400];
		Arrays.fill(ans, Integer.MAX_VALUE);
		if(reversible && isFalling(from / 25, from % 25))
			return ans;
		ArrayDeque <Integer> cola = new ArrayDeque <Integer> ();
		cola.clear();
		ans[from] = 0;
		cola.add(from);
		while(!cola.isEmpty())
		{
			int actualR = cola.poll();
			int actualDir = ans[actualR] & DIRECTION_MASK;
			int actualV = ans[actualR] & POSITION_MASK;
			for(int vecino : neighbors[actualR])
			{
				int vecinoR = vecino & POSITION_MASK;
				if(reversible && isFalling(vecinoR / 25, vecinoR % 25))
					continue;
				if(reversible && map[actualR / 25][actualR % 25] == 'H' && map[vecinoR / 25][vecinoR % 25] == '.' && ((vecino & DIRECTION_MASK) >>> 16) == DOWN)
					continue;
				if(actualV + 1 == (ans[vecinoR] & POSITION_MASK))
					ans[vecinoR] |= actualDir;
				else if(ans[vecinoR] == Integer.MAX_VALUE)
				{
					int dirV = actualR == from ? (vecino & DIRECTION_MASK) : actualDir;
					ans[vecinoR] = (actualV + 1) | dirV;
					cola.add(vecinoR);
				}
			}
		}
		return ans;
	}

	int convert(int i, int j, int dir)
	{
		return (i * 25 + j) + (dir << 16);
	}
	
	boolean isValid(int i, int j)
	{
		return i < 16 && i >= 0 && j < 25 && j >= 0 && map[i][j] != '=';
	}
	
	boolean isFalling(int i, int j)
	{
		return i + 1 < 16 && map[i][j] == '.' && map[i + 1][j] == '.';
	}
	
	int[] generateNeighbors(int from, ArrayList <Integer> all)
	{
		int i = from / 25;
		int j = from % 25;
		if(map[i][j] == '=')
			return neighbors[from] = new int[]{};
		if(isFalling(i, j))
			return neighbors[from] = new int[]{convert(i + 1, j, DOWN)};
		all.clear();
		if(map[i][j] == 'H')
		{
			for(int[] d : ladderDiffs)
				if(isValid(i + d[0], j + d[1]))
					all.add(convert(i + d[0], j + d[1], d[2]));
		}
		else if(i < 15 && map[i + 1][j] == 'H')
		{
			for(int[] d : normalLadderDiffs)
				if(isValid(i + d[0], j + d[1]))
					all.add(convert(i + d[0], j + d[1], d[2]));
		}
		else
		{
			for(int[] d : normalDiffs)
				if(isValid(i + d[0], j + d[1]))
					all.add(convert(i + d[0], j + d[1], d[2]));
		}
		int[] neigh = new int[all.size()];
		int indice = 0;
		for(int n : all) neigh[indice++] = n;
		return neighbors[from] = neigh;
	}
	
	private static boolean isValid(int i, int j, char[][] map)
	{
		return i < 16 && i >= 0 && j < 25 && j >= 0 && map[i][j] != '=';
	}
	
	boolean isFalling(int i, int j, char[][] map) 
	{
		return i + 1 < 16 && (map[i][j] == '.' || map[i][j] == '-') && (map[i + 1][j] == '.' || map[i + 1][j] == '-');
	}
	
	void generateNeighbors(int from, char[][] map, ArrayList <Integer> all)
	{
		int i = from / 25;
		int j = from % 25;
		all.clear();
		if(map[i][j] == '=')
			return;
		if(isFalling(i, j, map))
		{
			all.add(convert(i + 1, j, DOWN));
			return;
		}
		if(map[i][j] == 'H')
		{
			for(int[] d : ladderDiffs)
				if(isValid(i + d[0], j + d[1], map))
					all.add(convert(i + d[0], j + d[1], d[2]));
		}
		else if(i < 15 && map[i + 1][j] == 'H')
		{
			for(int[] d : normalLadderDiffs)
				if(isValid(i + d[0], j + d[1], map))
					all.add(convert(i + d[0], j + d[1], d[2]));
		}
		else
		{
			for(int[] d : normalDiffs)
				if(isValid(i + d[0], j + d[1], map))
					all.add(convert(i + d[0], j + d[1], d[2]));
		}
	}

	void generateNeighborsDigging(int from, char[][] map, boolean[][] trappedZombies, ArrayList <Integer> all)
	{
		int i = from / 25;
		int j = from % 25;
		all.clear();
		if(isFalling(i, j, map) || i >= 14 || map[i + 1][j] == '.' || map[i + 1][j] == '-') return;
		for(int diff : new int[]{1, -1})
		{
			int jN = j + diff;
			if(jN < 0 || jN >= 25) continue;
			if(map[i + 1][jN] == '=' && !trappedZombies[i + 1][jN] && !trappedZombies[i + 1][j] && map[i + 2][jN] != '=' && (map[i][jN] == '.' || map[i][jN] == '-'))
				all.add(convert(i + 2, jN, diff == -1 ? DIG_LEFT : DIG_RIGHT));
		}
	}
	
	int[] fill(int from, boolean reversible, UtilClasses.FastArrayDeque cola)
	{
		int[] ans = new int[400];
		Arrays.fill(ans, Integer.MAX_VALUE);
		if(reversible && isFalling(from / 25, from % 25))
			return ans;
		if(map[from / 25][from % 25] == '=')
			return ans;
		ans[from] = 0;
		cola.clear();
		cola.add(from);
		while(!cola.isEmpty())
		{
			int actualR = cola.poll();
			int actualDir = ans[actualR] & DIRECTION_MASK;
			int actualV = ans[actualR] & POSITION_MASK;
			for(int vecino : neighbors[actualR])
			{
				int vecinoR = vecino & POSITION_MASK;
				if(reversible && isFalling(vecinoR / 25, vecinoR % 25))
					continue;
				if(reversible && map[actualR / 25][actualR % 25] == 'H' && map[vecinoR / 25][vecinoR % 25] == '.' && ((vecino & DIRECTION_MASK) >>> 16) == DOWN)
					continue;
				if(actualV + 1 == (ans[vecinoR] & POSITION_MASK))
					ans[vecinoR] |= actualDir;
				else if(ans[vecinoR] == Integer.MAX_VALUE)
				{
					int dirV = actualR == from ? (vecino & DIRECTION_MASK) : actualDir;
					ans[vecinoR] = (actualV + 1) | dirV;
					cola.add(vecinoR);
				}
			}
		}
		return ans;
	}

	static final int[][] ansTmp = new int[400][100];
	static final boolean[][] trappedZombiesTmp = new boolean[400][100];
	static final ArrayList <Integer> tmpArray = new ArrayList <Integer> ();
	static final char[][] tmpAnterior = new char[16][25];
	
	synchronized int[] fillDiggingExact(int from, int dig, char[][] mapWithZombiesAndHoles, char[][] mapWithHoles)
	{
		char[][] anterior = tmpAnterior;
		boolean[][] trappedZombies = trappedZombiesTmp;
		for(int i = 0; i < 16; i++)
			for(int j = 0; j < 25; j++)
			{
				trappedZombies[i][j] = mapWithZombiesAndHoles[i][j] == 'Z' && map[i][j] == '=';
				if(trappedZombies[i][j])
				{
					anterior[i][j] = mapWithHoles[i][j];
					mapWithHoles[i][j] = '=';
				}
			}
		int[][] ans = ansTmp;
		for(int[] v : ans)
			Arrays.fill(v, Integer.MAX_VALUE);
		ArrayList <Integer> all = tmpArray;
		PriorityQueue <Entry> cola = new PriorityQueue <Entry> ();
		ans[from][dig] = 0;
		cola.add(new Entry(from, dig, 0));
		while(!cola.isEmpty())
		{
			Entry entrada = cola.poll();
			if((ans[entrada.pos][entrada.dig] & POSITION_MASK) != entrada.weight)
				continue;
			int actualR = entrada.pos;
			int actualD = entrada.dig;
			int actualDir = ans[actualR][actualD] & DIRECTION_MASK;
			int actualV = ans[actualR][actualD] & POSITION_MASK;
			int siguienteD = actualD == 0 ? 0 : actualD - 1;
			generateNeighbors(actualR, mapWithHoles, all);
			for(int vecino : all)
			{
				int vecinoR = vecino & POSITION_MASK;
				if(actualV + 1 == (ans[vecinoR][siguienteD] & POSITION_MASK))
					ans[vecinoR][siguienteD] |= actualDir;
				else if(actualV + 1 < (ans[vecinoR][siguienteD] & POSITION_MASK))
				{
					int dirV = actualR == from ? (vecino & DIRECTION_MASK) : actualDir;
					ans[vecinoR][siguienteD] = (actualV + 1) | dirV;
					cola.add(new Entry(vecinoR, siguienteD, actualV + 1));
				}
			}
			if(actualD == 0)
			{
				generateNeighborsDigging(actualR, mapWithHoles, trappedZombies, all);
				for(int vecino : all)
				{
					int vecinoR = vecino & POSITION_MASK;
					if(actualV + 4 == (ans[vecinoR][7] & POSITION_MASK))
						ans[vecinoR][7] |= actualDir;
					else if(actualV + 4 < (ans[vecinoR][7] & POSITION_MASK))
					{
						int dirV = actualR == from ? (vecino & DIRECTION_MASK) : actualDir;
						ans[vecinoR][7] = (actualV + 4) | dirV;
						cola.add(new Entry(vecinoR, 7, actualV + 4));
					}
				}
			}
		}
		int[] respuesta = new int[400];
		Arrays.fill(respuesta, Integer.MAX_VALUE);
		for(int i = 0; i < respuesta.length; i++)
		{
			for(int j = 0; j < 11; j++)
			{
				int r = ans[i][j] & POSITION_MASK;
				if(r < (respuesta[i] & POSITION_MASK))
					respuesta[i] = ans[i][j];
			}
		}
		for(int i = 0; i < 16; i++)
			for(int j = 0; j < 25; j++)
				if(trappedZombies[i][j])
					mapWithHoles[i][j] = anterior[i][j];	
		return respuesta;
	}
	
	final UtilClasses.FastArrayDeque tmpQueue = new UtilClasses.FastArrayDeque(400);

	int getReversible(int fromI, int fromJ, int toI, int toJ)
	{
		int idFrom = fromI * 25 + fromJ;
		if(shortestReversiblePath[idFrom] == null) shortestReversiblePath[idFrom] = fill(idFrom, true, tmpQueue);
		return shortestReversiblePath[idFrom][toI * 25 + toJ];
	}
	
	int getFalling(int fromI, int fromJ, int toI, int toJ)
	{
		int idFrom = fromI * 25 + fromJ;
		if(shortestFallingPath[idFrom] == null) shortestReversiblePath[idFrom] = fill(idFrom, false, tmpQueue);
		return shortestFallingPath[idFrom][toI * 25 + toJ];
	}
	
	int[] getDiggingExact(int fromI, int fromJ, int dig, char[][] mapWithZombiesAndHoles, char[][] mapWithHoles)
	{
		int idFrom = fromI * 25 + fromJ;
		return fillDiggingExact(idFrom, dig, mapWithZombiesAndHoles, mapWithHoles);
	}
	
	int fallsInto(int fromI, int fromJ)
	{
		while(fromI < 15 && map[fromI + 1][fromJ] == '.')
			fromI++;
		return fromI;
	}
	
	int fallTime(int fromI, int fromJ)
	{
		int fallTime = 0;
		while(fromI < 15 && map[fromI + 1][fromJ] == '.')
		{
			fromI++;
			fallTime++;
		}
		return fallTime;
	}
	
	int convert2(int i, int j, int dig, int floor, int dir)
	{
		return (i * 25 + j) + (dig << 9) + (floor << 15) + (dir << 20);
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
	
	int[] ansTmp2;
	static final int MASK_POS_FROM = (1 << 20) - 1;
	static final int MASK_DIR = ~MASK_POS_FROM;
	ArrayList <Integer> allTmp = new ArrayList <Integer> ();
	UtilClasses.FastArrayDeque colaTmp;
	int[][] shortestDiggingPath;
	
	synchronized int[] fillDigging(int from, int dig, int floor)
	{
		int[] ans = ansTmp2;
		Arrays.fill(ans, Integer.MAX_VALUE);
		UtilClasses.FastArrayDeque cola = colaTmp;
		cola.clear();
		int indiceInicio = convert2(from / 25, from % 25, dig, floor, 0);
		ans[indiceInicio] = 0;
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
				if(actualV + 1 == (ans[vecinoR] & POSITION_MASK))
					ans[vecinoR] |= actualDir;
				else if(ans[vecinoR] == Integer.MAX_VALUE)
				{
					int dirV = entrada == indiceInicio ? (((vecino & MASK_DIR) >>> 20) << 16) : actualDir;
					ans[vecinoR] = (actualV + 1) | dirV;
					cola.add(vecinoR);
				}
			}
		}
		int[] respuesta = new int[400];
		Arrays.fill(respuesta, Integer.MAX_VALUE);
		for(int i = 0; i < 16; i++)
			for(int j = 0; j < 25; j++)
				for(int d = 0; d <= 10; d++)
					for(int f = 0; f < 5; f++)
					{
						int r = ans[convert2(i, j, d, f, 0)] & POSITION_MASK;
						if(r < (respuesta[i * 25 + j] & POSITION_MASK))
							respuesta[i * 25 + j] = ans[convert2(i, j, d, f, 0)];
					}
		return respuesta;
	}
	
	synchronized int[] getDiggingFast(int i, int j, int dig, char[][] mapWithZombiesAndHoles)
	{
		int floor = getFloor(i, j, mapWithZombiesAndHoles);
		int indice = convert2(i, j, dig, floor, 0);
		if(ansTmp2 == null)
		{
			ansTmp2 = new int[1 << 18];
			colaTmp = new UtilClasses.FastArrayDeque(1 << 18);
			shortestDiggingPath = new int[1 << 18][];
		}
		if(shortestDiggingPath[indice] == null) shortestDiggingPath[indice] = fillDigging(i * 25 + j, dig, floor);
		return shortestDiggingPath[indice];	
	}
	
	synchronized int[] getDiggingFast(int i, int j, int dig, UtilClasses.FastHash mapWithZombiesAndHoles, Game.GameEnemy[] enemies)
	{
		int floor = getFloor(i, j, mapWithZombiesAndHoles, enemies);
		int indice = convert2(i, j, dig, floor, 0);
		if(ansTmp2 == null)
		{
			ansTmp2 = new int[1 << 18];
			colaTmp = new UtilClasses.FastArrayDeque(1 << 18);
			shortestDiggingPath = new int[1 << 18][];
		}
		if(shortestDiggingPath[indice] == null) shortestDiggingPath[indice] = fillDigging(i * 25 + j, dig, floor);
		return shortestDiggingPath[indice];	
	}

	static int getDistance(int i, int j, int[] distanceArray)
	{
		return distanceArray[i * 25 + j] & POSITION_MASK;
	}
	
	static int getDirection(int i, int j, int[] distanceArray)
	{
		return getDirection(distanceArray[i * 25 + j]);
	}
	
	static int getDistance(int mask)
	{
		return mask & POSITION_MASK;
	}
	
	int getFloor(int i, int j, char[][] mapWithZombiesAndHoles)
	{
		if(mapWithZombiesAndHoles[i][j] == '-') return 4;
		else if(i < 15 && j >= 1 && mapWithZombiesAndHoles[i + 1][j - 1] == '-') return 1;
		else if(i < 15 && mapWithZombiesAndHoles[i + 1][j] == '-') return 2;
		else if(i < 15 && j < 24 && mapWithZombiesAndHoles[i + 1][j + 1] == '-') return 3;
		else return 0;
	}
	
	int getFloor(int i, int j, UtilClasses.FastHash mapWithZombiesAndHoles, Game.GameEnemy[] enemies)
	{
		if(mapWithZombiesAndHoles.get(i, j) == '-') return 4;
		else if(i < 15 && j >= 1 && mapWithZombiesAndHoles.get(i + 1, j - 1) == '-' && !enemyIn(enemies, i + 1, j - 1)) return 1;
		else if(i < 15 && mapWithZombiesAndHoles.get(i + 1, j) == '-' && !enemyIn(enemies, i + 1, j)) return 2;
		else if(i < 15 && j < 24 && mapWithZombiesAndHoles.get(i + 1, j + 1) == '-' && !enemyIn(enemies, i + 1, j + 1)) return 3;
		else return 0;
	}
	
	private boolean enemyIn(Game.GameEnemy[] enemies, int i, int j) 
	{
		for(Game.GameEnemy e : enemies) if(e.i == i && e.j == j) return true;
		return false;
	}

	static int getDirection(int dir)
	{
		dir >>>= 16;
		if((dir & OriginalMap.UP) != 0)
			return Game.TOP;
		else if((dir & OriginalMap.DOWN) != 0)
			return Game.BOTTOM;
		else if((dir & OriginalMap.RIGHT) != 0)
			return Game.RIGHT;
		else if((dir & OriginalMap.LEFT) != 0)
			return Game.LEFT;
		else if((dir & OriginalMap.DIG_LEFT) != 0)
			return Game.DIG_LEFT;
		else if((dir & OriginalMap.DIG_RIGHT) != 0)
			return Game.DIG_RIGHT;
		else if((dir & OriginalMap.SUICIDE) != 0)
			return Game.SUICIDE;
		else
			return Game.NONE;
	}
}
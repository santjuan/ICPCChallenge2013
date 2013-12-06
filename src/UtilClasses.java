import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class UtilClasses
{
	static class Scanner
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		StringTokenizer st = new StringTokenizer("");
		
		public String nextLine()
		{
			try
			{
				return br.readLine();
			}
			catch(Exception e)
			{
				throw(new RuntimeException());
			}
		}
		
		public String next()
		{
			while(!st.hasMoreTokens())
			{
				String l = nextLine();
				if(l == null)
					return null;
				st = new StringTokenizer(l);
			}
			return st.nextToken();
		}
		
		public int nextInt()
		{
			return Integer.parseInt(next());
		}
	}
	
	public static class FastStack
	{
		long[] values = new long[2];
		int size = 0;
		
		void push(int val)
		{
			ensureCapacity();
			int word = size >>> 5;
			int pos = size & 31;
			put(word, pos << 1, val);
			size++;
		}

		void put(int word, int pos, long val)
		{
			values[word] &= ~(3L << pos);
			values[word] |= val << pos;
		}
		
		int pop()
		{
			int word = (size - 1) >>> 5;
			int pos = (size - 1) & 31;
			size--;
			return (int) ((values[word] >>> (pos << 1)) & 3);
		}
		
		void ensureCapacity()
		{
			if((size & 31) != 0)
				return;
			int wordSize = (size) >>> 5;
			if(values.length > wordSize)
				values[wordSize] = 0;
			else
			{
				long[] newValues = new long[wordSize + 1];
				for(int i = 0; i < wordSize; i++) newValues[i] = values[i];
				newValues[wordSize] = 0;
				values = newValues;
			}
		}
		
		FastStack clonar()
		{
			FastStack clon = new FastStack();
			clon.size = size;
			clon.values = values.clone();
			return clon;
		}
	}
	
	public static class FastHash
	{
		private class Entrada
		{
			int id;
			char val;
		}
	
		char[][] originalMap;
		Entrada[][] hash = new Entrada[16][25];
		int currentId;
		
		FastHash(char[][] oM)
		{
			for(int i = 0; i < 16; i++)
				for(int j = 0; j < 25; j++) 
				{ 
					hash[i][j] = new Entrada();
					hash[i][j].id = -1;
				}
			currentId = 0;
			originalMap = oM;
		}
		
		void put(int i, int j, char val)
		{
			Entrada e = hash[i][j];
			e.id = currentId;
			e.val = val;
		}
		
		char get(int i, int j)
		{
			Entrada e = hash[i][j];
			if(e.id != currentId) return originalMap[i][j];
			else return e.val;
		}
		
		void clear()
		{
			currentId++;
		}
	}
	
	public static class FastBitSet
	{
		long[] values = new long[7];
		
		void set(int index)
		{
			int wordIndex = index >>> 6;
			int bitIndex = index & 63;
			values[wordIndex] |= 1L << bitIndex;
		}
		
		boolean get(int index)
		{
			int wordIndex = index >>> 6;
			int bitIndex = index & 63;
			return (values[wordIndex] >>> bitIndex & 1) == 1;
		}
		
		FastBitSet clonar()
		{
			FastBitSet n = new FastBitSet();
			n.values = values.clone();
			return n;
		}
	}
	
	static class FastArrayDeque
	{
		final int[] queue;
		int head, tail;
		
		FastArrayDeque(int size)
		{
			queue = new int[size];
			clear();
		}
		
		
		void add(int val)
		{
			queue[++tail] = val;
		}
		
		int poll()
		{
			return queue[head++];
		}
		
		void clear()
		{
			head = 0; tail = -1;
		}
		
		boolean isEmpty()
		{
			return head > tail;
		}
	}
}
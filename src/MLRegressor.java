import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class MLRegressor implements MLAdvisor.Regressor
{

	int H, h, D, d;
	double[] V;
	double[][] W;
	double[] med;
	double[] dev;
	
	MLRegressor(String file)
	{
		try
		{
			Scanner sc = new Scanner(new File(file));
			d = sc.nextInt();
			h = sc.nextInt();
			med = new double[d];
			for(int i = 0; i < d; i++) med[i] = sc.nextDouble();
			dev = new double[d];
			for(int i = 0; i < d; i++) dev[i] = sc.nextDouble();
			H = h + 1;
			V = new double[H];
			for(int i = 0; i < V.length; i++) V[i] = sc.nextDouble();
			D = d + 1;
			W = new double[h][D];
			for(int i = 0; i < h; i++)
				for(int j = 0; j < D; j++)
					W[i][j] = sc.nextDouble();
			sc.close();
		}
		catch (FileNotFoundException e) 
		{
			throw new RuntimeException();
		}
	}
	
	double dot(double[] x, double[] w)
	{
		double ans = w[0];
		for(int i = 1; i < w.length; i++)
			ans += x[i - 1] * w[i];
		return ans;
	}
	
	double sigm(double x)
	{
		return 1 / (1 + Math.exp(-x));
	}
	
	double predict(double[] x)
	{
		for(int i = 0; i < x.length; i++)
			x[i] = (x[i] - med[i]) / dev[i];
		double[] z = new double[h];
		for(int i = 0; i < h; i++)
			z[i] = sigm(dot(x, W[i]));
		return dot(z, V);
	}

	@Override
	public double getReward(MLAdvisorBase.MLEntry entry)
	{
		double[] x = entry.getVals();
		return predict(x);
	}
}
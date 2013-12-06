import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class NearGoldNearEnemyTestDebug extends Game.Strategy
{
	public static void empezar()
	{
		JFrame frame = new JFrame("Debug");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JButton button = new JButton("Turno");
        button.setPreferredSize(new Dimension(175, 100));
        frame.getContentPane().add(button, BorderLayout.CENTER);
        JButton button2 = new JButton("Varios Turnos");
        button2.setPreferredSize(new Dimension(175, 100));
        frame.getContentPane().add(button2, BorderLayout.SOUTH);
        button.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) 
			{
				turnos.add(0);
			}
		});
        button2.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) 
			{
				 int val = Integer.parseInt(JOptionPane.showInputDialog("Ingrese el numero de turnos a saltarse"));
				 for(int i = 0; i < val; i++)
					 turnos.add(0);
			}
		});
        frame.pack();
        frame.setVisible(true);
	}
	
	static final ArrayBlockingQueue<Integer> turnos = new ArrayBlockingQueue <Integer> (10000);
	
	public static void main(String[] args) throws InterruptedException
	{
		empezar();
		NearGoldNearEnemyTestDebug strategy = new NearGoldNearEnemyTestDebug();
		RealtimeState state = new RealtimeState();
		strategy.init(state, state.initial, true);
		state.readTurn(-1);
		while(state.currentTurn >= 0)
		{
			turnos.poll(1000000, TimeUnit.SECONDS);
			state.readTurn(strategy.getPlay(state));
		}
	}

	@Override
	void init(Game state, Game.InitialState initialState, boolean isRed) 
	{
		gold = new NearGoldAdvisor();
		gold.init(initialState, state, isRed);
		defense = new BruteForceDefense(6);
		defense.init(initialState, state, true);
	}
}
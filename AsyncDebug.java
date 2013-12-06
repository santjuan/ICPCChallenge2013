import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;


public class AsyncDebug 
{
	 private void createAndShowGUI() {
	        //Create and set up the window.
	        JFrame frame = new JFrame("FrameDemo");
	        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	        frame.setPreferredSize(new Dimension(200, 200));
	        JButton button = new JButton("Continuar");
	        button.addActionListener(new ActionListener() {
				int lastAction = -1;
				
				@Override
				public void actionPerformed(ActionEvent arg0)
				{
					state.readTurn(lastAction);
					lastAction = Simulator.NONE;
					while(state.currentTurn < firstImportant)
						state.readTurn(lastAction);
				}
			});
	        frame.getContentPane().add(button, BorderLayout.CENTER);
	 
	        //Display the window.
	        frame.pack();
	        frame.setVisible(true);
	    }
	 
	final RealtimeState state = new RealtimeState();
	final int firstImportant;
	
	public AsyncDebug(int f)
	{		
		createAndShowGUI();
		firstImportant = f;
	}
	
	public static void main(String[] args)
	{
		new AsyncDebug(args.length == 0 ? -1 : Integer.parseInt(args[0]));
	}
}
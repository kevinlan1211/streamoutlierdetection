import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;

public class P2 {
	static ArrayList<double[]> window;
	static ArrayList<Double> knn_distances;
	static ArrayList<Double> ars;
	static ArrayList<Double> LOF_scores;
	static ArrayList<HashSet<Integer>> adj_list;
 	static int K;
	static double T;
	
	
	public static double distance(int index1, int index2) {
		double[] pt1 = window.get(index1);
		double[] pt2 = window.get(index2);
		
		double squared_distance = 0;
		
		for(int i = 0; i < pt1.length; i++) {
			squared_distance += Math.pow((pt1[i] - pt2[i]), 2);
		}
		
		return Math.sqrt(squared_distance);
	}
	
	public static double distance(double[] pt1, int index2) {
		double[] pt2 = window.get(index2);
		
		double squared_distance = 0;
		
		for(int i = 0; i < pt1.length; i++) {
			squared_distance += Math.pow((pt1[i] - pt2[i]), 2);
		}
		
		return Math.sqrt(squared_distance);
	}
	
	public static void kNNDistance(int index) {
		int window_size = window.size();
		double[] distances = new double[window_size];
		for(int i = 0; i < window_size; i++) {
			distances[i] = distance(index, i);
		}
		
		Arrays.sort(distances);
		double knn_distance = distances[Math.min(K, window_size - 1)];
		knn_distances.set(index, knn_distance);
		
		updateAdjList(index);
	}
	
	public static void updateAdjList(int index) {
		adj_list.get(index).clear();
		for(int i = 0; i < window.size(); i++) {
			if(distance(index, i) <= knn_distances.get(index) && i != index) {
				adj_list.get(index).add(i);
			}
		}
	}
	
	public static void ar(int index) {
		double total_r = 0;
		int window_size = window.size();
		int count = 0;
		for(int i = 0; i < window_size; i++) {
			double d = distance(index, i);
			if(adj_list.get(index).contains(i)) {
				total_r += Math.max(d, knn_distances.get(i));
				count++;
			}
		}
		if(count > 0)
			ars.set(index, total_r / count);
		
	}
	
	public static void LOF(int index) {
		double total_sum = 0;
		int window_size = window.size();
		int count = 0;
		double arx = ars.get(index);
		for(int i = 0; i < window_size; i++) {
			if(adj_list.get(index).contains(i)) {
				total_sum += arx / ars.get(i);
				count++;
			}
		}
		
		if(count > 0)
			LOF_scores.set(index, total_sum / count);
	}
	
	public static double[] parseLine(String line) {
		Scanner double_sc = new Scanner(line);
		double_sc.useDelimiter(",");
		
		int commas = 0;
		for(int i = 0; i < line.length(); i++) {
			if(line.charAt(i) == ',')
				commas++;
		}
		
		int dim = commas + 1;
		
		double[] result = new double[dim];
		
		int index = 0;
		
		while(double_sc.hasNext()) {
			if(double_sc.hasNextDouble()) {
				double d = double_sc.nextDouble();
				result[index] = d;
				index++;
			}
			else {
				double_sc.next();
			}
		}
		
		return result;
	}
	
	public static void main(String[] args) throws NumberFormatException, IOException{
		window = new ArrayList<double[]>();
		knn_distances = new ArrayList<Double>();
		ars = new ArrayList<Double>();
		LOF_scores = new ArrayList<Double>();
		adj_list = new ArrayList<HashSet<Integer>>();
		int MAX_WINDOW_SIZE = 100;
		K = 20;
		T = 2.5;
		

		BufferedReader stdinReader = new BufferedReader(new InputStreamReader(System.in));
		MAX_WINDOW_SIZE = Integer.parseInt(stdinReader.readLine().trim());
		String[] connection_args = stdinReader.readLine().split(":");
		
		int removed_count = 0;
		try {
			Socket socket = new Socket(connection_args[0].trim(), Integer.parseInt(connection_args[1].trim()));
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			String line;
			int total_index = 0;
			while((line = reader.readLine()) != null) {
				double[] pt = parseLine(line);
				
				window.add(pt);
				knn_distances.add((double) 1);
				ars.add((double) 1);
				LOF_scores.add((double) 1);
				adj_list.add(new HashSet<Integer>());
				
				double[] removed_point = null;
				boolean removed = false;
				if(window.size() > MAX_WINDOW_SIZE) {
					removed_point = window.get(0);
					if(LOF_scores.get(0) > T && removed_count > MAX_WINDOW_SIZE) {
						System.out.println("Outlier point for threshold " + T + ": " + Arrays.toString(removed_point));
						System.out.println("Point index: " + removed_count);
						System.out.println("LOF score: " + LOF_scores.get(0));
					}
					
					window.remove(0);
					knn_distances.remove(0);
					ars.remove(0);
						
					LOF_scores.remove(0);
					adj_list.remove(0);
					removed = true;
					removed_count++;
				}
				
				int window_size = window.size();
				int new_pt_index = window_size - 1;
				
				HashSet<Integer> modified = new HashSet<Integer>();
				
				for(int i = 0; i < window_size; i++) {
					int rpi;
					double d = distance(new_pt_index, i);
					
					if(removed_point != null)
						rpi = 0;
					else 
						rpi = -1;
					if(adj_list.get(i).contains(0) || d <= knn_distances.get(i) || i == new_pt_index) {
						kNNDistance(i);
						modified.add(i);
					}
					else {
						updateAdjList(i);
					}
				}

				HashSet<Integer> modified2 = new HashSet<Integer>();
				for(int i = 0; i < window_size; i++) {
					HashSet<Integer> intersection = new HashSet<Integer>(modified);
					intersection.retainAll(adj_list.get(i));
					if(modified.contains(i) || intersection.size() != 0) {
						ar(i);
						modified2.add(i);
					}
				}
				
				modified.addAll(modified2);
				
				for(int i = 0; i < window_size; i++) {
					HashSet<Integer> intersection = new HashSet<Integer>(modified);
					intersection.retainAll(adj_list.get(i));
					if(modified.contains(i) || intersection.size() != 0) {
						LOF(i);
					}
				}
				/*LOF(new_pt_index);
				
				if(LOF_scores.get(new_pt_index) > T && removed == true) {
					System.out.println("Outlier point for threshold " + T + ": " + Arrays.toString(window.get(new_pt_index)));
					System.out.println("Point index: " + total_index);
					System.out.println("LOF score: " + LOF_scores.get(new_pt_index));
				}*/
				
				total_index++;
			}
			socket.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		int index = 0;
		for(double[] point:window) {
			if(LOF_scores.get(index) > T) {
				System.out.println("Outlier point for threshold " + T + ": " + Arrays.toString(point));
				System.out.println("Point index: " + (removed_count + index));
				System.out.println("LOF score: " + LOF_scores.get(index));
			}
			index++;
		}
	}
}

import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
interface UtilityProvider {
    void distributeUtilities(City city);
}


interface ServiceProvider {
    void applyServices(City city);
}


interface Upgrade {
    void updateBuilding(City city);
}


interface ResourceProducer {
    void calculateProduction(City city);
}

interface Connectable {
    boolean canConnect();
}
interface BuildingAction {
    void apply(Building b);
}

public class ObjectVilleGame {
    public static void main(String[] args) {
        String mapFile;
        if(args.length >= 1){
            mapFile = args[0];
        }else{
            mapFile = "map00.txt";
        }

        int ticks;
        if(args.length >= 2){
            ticks = Integer.parseInt(args[1]);
        }else{
            ticks = 10;
        }

        Building[][] mainArray = MapLoader.loadFromFile(mapFile);

        City city = new City(mainArray);

        for(int i = 1; i <= ticks; i++){
            city.tick(i);
        }
    }
}

class City {

    Building[][] grid;
    private final int rows;
    private final int cols;

    private int populationStorage;
    private int goodsStorage;
    private int lifestyleStorage;


    private int nextPopulationStorage;
    private int nextGoodsStorage;
    private int nextLifestyleStorage;

    City(Building[][] grid) {
        this.grid = grid;
        this.rows = grid.length;
        if (this.rows > 0) {
            this.cols = grid[0].length;
        } else {
            this.cols = 0;
        }
    }
    private void forEachBuilding(BuildingAction action) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (grid[i][j] != null) {

                    action.apply(grid[i][j]);
                }
            }
        }
    }

    public boolean coordinateChecker(int userRow, int userCol) {
        return userRow >= 0 && userRow < rows && userCol >= 0 && userCol < cols;
    }

    public Building buildingInThatCoordinate(int userRow, int userCol) {
        if (coordinateChecker(userRow, userCol)) {
            return grid[userRow][userCol];
        }
        return null;
    }

    public void tick(int tickNumber) {
        System.out.println("Tick " + tickNumber);

        populationStorage = nextPopulationStorage;
        goodsStorage = nextGoodsStorage;
        lifestyleStorage = nextLifestyleStorage;

        nextPopulationStorage = 0;
        nextGoodsStorage = 0;
        nextLifestyleStorage = 0;


        forEachBuilding(b -> b.resetForNewTick());


        forEachBuilding(b -> {
            if (b instanceof ServiceProvider) {
                ((ServiceProvider) b).applyServices(this);
            }
        });


        forEachBuilding(b -> {
            if (b instanceof UtilityProvider) {
                ((UtilityProvider) b).distributeUtilities(this);
            }
        });

        distributeResources();


        forEachBuilding(b -> {
            if (b instanceof Upgrade) {
                ((Upgrade) b).updateBuilding(this);
            }
        });


        forEachBuilding(b -> {
            if (b instanceof ResourceProducer) {
                ((ResourceProducer) b).calculateProduction(this);
            }
        });
    }

    private void distributeResources() {

        int numberOfHouse = 0;
        int numberOfIndustrial = 0;
        int numberOfCommercial = 0;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (grid[row][col] instanceof House) {
                    numberOfHouse++;
                } else if (grid[row][col] instanceof Industrial) {
                    numberOfIndustrial++;
                } else if (grid[row][col] instanceof Commercial) {
                    numberOfCommercial++;
                }
            }
        }

        int sharedPopulation = 0;
        int numberOfPopulationReceivers = numberOfIndustrial + numberOfCommercial;
        if (populationStorage > 0 && numberOfPopulationReceivers > 0) {
            sharedPopulation = populationStorage / numberOfPopulationReceivers;
        }

        int sharedGoods = 0;
        if (goodsStorage > 0 && numberOfCommercial > 0) {
            sharedGoods = goodsStorage / numberOfCommercial;
        }

        int sharedLifeStyle = 0;
        if (lifestyleStorage > 0 && numberOfHouse > 0) {
            sharedLifeStyle = lifestyleStorage / numberOfHouse;
        }

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {

                Building building = grid[row][col];

                if (building instanceof House) {
                    House house = (House) building;

                    if (sharedLifeStyle > 0) {
                        house.setCurrentLifestyle(sharedLifeStyle);
                        System.out.println("House at (" + row + "," + col + ") received " + sharedLifeStyle + " lifestyle");
                    }

                } else if (building instanceof Commercial) {
                    Commercial commercial = (Commercial) building;

                    if (sharedPopulation > 0) {
                        commercial.setCurrentPopulation(sharedPopulation);
                        System.out.println("Commercial at (" + row + "," + col + ") received " + sharedPopulation + " population");
                    }

                    if (sharedGoods > 0) {
                        commercial.setCurrentGoods(sharedGoods);
                        System.out.println("Commercial at (" + row + "," + col + ") received " + sharedGoods + " goods");
                    }

                } else if (building instanceof Industrial) {
                    Industrial industrial = (Industrial) building;

                    if (sharedPopulation > 0) {
                        industrial.setCurrentPopulation(sharedPopulation);
                        System.out.println("Industrial at (" + row + "," + col + ") received " + sharedPopulation + " population");
                    }
                }
            }
        }
    }

    public void deliverBFS(int startRow, int startCol, String utilityType, int capacity) {

        boolean[][] hasExploredLocation = new boolean[rows][cols];
        ArrayList<int[]> coordinates = new ArrayList<int[]>();



        hasExploredLocation[startRow][startCol] = true;
        addNeighborsToList(startRow, startCol, coordinates, hasExploredLocation);

        while (coordinates.size() > 0 && capacity > 0) {

            int[] currentPosition = coordinates.remove(0);
            int currentRow = currentPosition[0];
            int currentCol = currentPosition[1];

            Building building = grid[currentRow][currentCol];

            if (building instanceof House || building instanceof Industrial || building instanceof Commercial) {

                if (utilityType.equals("internet") && building instanceof Industrial) {
                    addNeighborsToList(currentRow, currentCol, coordinates, hasExploredLocation);
                    continue;
                }

                int alreadyReceived = 0;

                if (utilityType.equals("electricity")) {
                    alreadyReceived = building.getElectricity();
                } else if (utilityType.equals("water")) {
                    alreadyReceived = building.getWater();
                } else if (utilityType.equals("internet")) {
                    alreadyReceived = building.getInternet();
                }

                int remainingDemand = building.demand - alreadyReceived;

                if (remainingDemand < 0) {
                    remainingDemand = 0;
                }

                if (remainingDemand > 0) {

                    int give = remainingDemand;

                    if (give > capacity) {
                        give = capacity;
                    }

                    if (utilityType.equals("electricity")) {
                        building.calculateReceivedElectricity(give);
                    } else if (utilityType.equals("water")) {
                        building.calculateReceivedWater(give);
                    } else if (utilityType.equals("internet")) {
                        building.calculateReceivedInternet(give);
                    }

                    String zoneName = "";

                    if (building instanceof House) {
                        zoneName = "House";
                    } else if (building instanceof Industrial) {
                        zoneName = "Industrial";
                    } else if (building instanceof Commercial) {
                        zoneName = "Commercial";
                    }

                    System.out.println(zoneName + " at (" + currentRow + "," + currentCol + ") received " + give + " " + utilityType);

                    capacity -= give;
                }
            }

            addNeighborsToList(currentRow, currentCol, coordinates, hasExploredLocation);
        }
    }

    private void addNeighborsToList(int row, int col, ArrayList<int[]> coordinates, boolean[][] visited) {

        if (row - 1 >= 0 && grid[row - 1][col] != null && grid[row - 1][col] instanceof Connectable) {
            if (((Connectable) grid[row - 1][col]).canConnect() && !visited[row - 1][col]) {
                visited[row - 1][col] = true;
                coordinates.add(new int[]{row - 1, col});
            }
        }

        if (row + 1 < rows && grid[row + 1][col] != null && grid[row + 1][col] instanceof Connectable) {
            if (((Connectable) grid[row + 1][col]).canConnect() && !visited[row + 1][col]) {
                visited[row + 1][col] = true;
                coordinates.add(new int[]{row + 1, col});
            }
        }

        if (col - 1 >= 0 && grid[row][col - 1] != null && grid[row][col - 1] instanceof Connectable) {
            if (((Connectable) grid[row][col - 1]).canConnect() && !visited[row][col - 1]) {
                visited[row][col - 1] = true;
                coordinates.add(new int[]{row, col - 1});
            }
        }

        if (col + 1 < cols && grid[row][col + 1] != null && grid[row][col + 1] instanceof Connectable) {
            if (((Connectable) grid[row][col + 1]).canConnect() && !visited[row][col + 1]) {
                visited[row][col + 1] = true;
                coordinates.add(new int[]{row, col + 1});
            }
        }
    }

    public void addToPopulationStorage(int amount) {
        nextPopulationStorage += amount;
    }

    public void addToGoodsStorage(int amount) {
        nextGoodsStorage += amount;
    }

    public void addToLifestyleStorage(int amount) {
        nextLifestyleStorage += amount;
    }
}

class MapLoader {

    public static Building[][] loadFromFile(String nameOfFile){

        try{

            File readFile = new File(nameOfFile);

            Scanner sizeReader = new Scanner(readFile);

            int rowCount = 0;
            int colCount = 0;

            while(sizeReader.hasNextLine()){

                String line = sizeReader.nextLine();

                rowCount++;

                if(line.length() > colCount){
                    colCount = line.length();
                }
            }

            sizeReader.close();

            Building[][] map = new Building[rowCount][colCount];

            Scanner reader = new Scanner(readFile);

            int row = 0;

            while(reader.hasNextLine()){

                String nextRow = reader.nextLine();

                for(int col = 0 ; col < nextRow.length(); col++ ){

                    char eachLetter = nextRow.charAt(col);

                    switch (eachLetter){

                        case 'E':
                            map[row][col] = new Empty(row,col);
                            break;

                        case 'H':
                            map[row][col] = new House(row,col);
                            break;

                        case 'R':
                            map[row][col] = new Road(row,col);
                            break;

                        case 'C':
                            map[row][col] = new Commercial(row,col);
                            break;

                        case 'I':
                            map[row][col] = new Industrial(row,col);
                            break;

                        case 'S':
                            map[row][col] = new School(row,col);
                            break;

                        case 'F':
                            map[row][col] = new PoliceStation(row,col);
                            break;

                        case 'D':
                            map[row][col] = new Hospital(row,col);
                            break;

                        case 'P':
                            map[row][col] = new PowerPlant(row,col);
                            break;

                        case 'W':
                            map[row][col] = new WaterPlant(row,col);
                            break;

                        case 'T':
                            map[row][col] = new InternetProvider(row,col);
                            break;
                    }
                }

                row++;
            }

            reader.close();

            return map;

        } catch (FileNotFoundException e ) {

            System.out.println("The document has not been found!");

            return new Building[0][0];
        }
    }
}
abstract class Building {

    protected final int row;
    protected final int col;

    protected char theSymbolInMap;
    protected String buildingName;
    protected int    buildingLevel;

    protected boolean isEducation;
    protected boolean isSecurity;
    protected boolean isHealth;

    protected int internet;
    protected int water;
    protected int electricity;


    protected int output = 0;
    protected int demand = 1;

    Building(int row, int col, char theSymbolInMap, String buildingName) {
        this.row           = row;
        this.col           = col;
        this.theSymbolInMap = theSymbolInMap;
        this.buildingName  = buildingName;
        this.buildingLevel = 0;
        this.isEducation   = false;
        this.isSecurity    = false;
        this.isHealth      = false;
        this.internet      = 0;
        this.water         = 0;
        this.electricity   = 0;
    }

    public void resetForNewTick() {
        isEducation = false;
        isSecurity  = false;
        isHealth    = false;
        electricity = 0;
        water       = 0;
        internet    = 0;
    }


    public void calculateReceivedWater(int amount) {
        this.water = water + amount;
    }
    public void calculateReceivedElectricity(int amount) {
        this.electricity = electricity + amount;
    }
    public void calculateReceivedInternet(int amount) {
        this.internet = internet + amount;
    }


    public int getElectricity() {
        return this.electricity;
    }

    public int getWater() {
        return this.water;
    }

    public int getInternet() {
        return this.internet;
    }
}
class School extends Building implements ServiceProvider {

    public static final int radius = 4;

    public School(int row, int col) {
        super(row, col, 'S', "School");
    }

    @Override
    public void applyServices(City city) {
        for (int r = row - radius; r <= row + radius; r++) {
            for (int c = col - radius; c <= col + radius; c++) {
                double distance = Math.sqrt(Math.pow(r - row, 2) + Math.pow(c - col, 2));
                if (distance <= radius) {
                    Building building = city.buildingInThatCoordinate(r, c);
                    if (building instanceof House) {
                        building.isEducation = true;
                        System.out.println("House at (" + r + "," + c + ") received education service");
                    }
                }
            }
        }
    }
}

class PoliceStation extends Building implements ServiceProvider {

    public static final int radius = 5;

    public PoliceStation(int row, int col) {
        super(row, col, 'F', "Police Station");
    }

    @Override
    public void applyServices(City city) {
        for (int r = row - radius; r <= row + radius; r++) {
            for (int c = col - radius; c <= col + radius; c++) {
                double distance = Math.sqrt(Math.pow(r - row, 2) + Math.pow(c - col, 2));
                if (distance <= radius) {
                    Building building = city.buildingInThatCoordinate(r, c);
                    if (building instanceof House) {
                        building.isSecurity = true;
                        System.out.println("House at (" + r + "," + c + ") received security service");
                    } else if (building instanceof Industrial) {
                        building.isSecurity = true;
                        System.out.println("Industrial at (" + r + "," + c + ") received security service");
                    } else if (building instanceof Commercial) {
                        building.isSecurity = true;
                        System.out.println("Commercial at (" + r + "," + c + ") received security service");
                    }
                }
            }
        }
    }
}

class Hospital extends Building implements ServiceProvider {

    public static final int radius = 3;

    public Hospital(int row, int col) {
        super(row, col, 'D', "Hospital");
    }

    @Override
    public void applyServices(City city) {
        for (int r = row - radius; r <= row + radius; r++) {
            for (int c = col - radius; c <= col + radius; c++) {
                double distance = Math.sqrt(Math.pow(r - row, 2) + Math.pow(c - col, 2));
                if (distance <= radius) {
                    Building building = city.buildingInThatCoordinate(r, c);
                    if (building instanceof House) {
                        building.isHealth = true;
                        System.out.println("House at (" + r + "," + c + ") received health service");
                    }
                }
            }
        }
    }
}
class House extends Building implements Connectable, Upgrade, ResourceProducer {

    private int currentLifestyle;

    public House(int row, int col) {
        super(row, col, 'H', "House");
        this.currentLifestyle = 0;
    }

    @Override
    public boolean canConnect() { return true; }

    @Override
    public void resetForNewTick() {
        super.resetForNewTick();
        currentLifestyle = 0;
    }

    public void setCurrentLifestyle(int amount) {
        this.currentLifestyle = amount;
    }

    @Override
    public void updateBuilding(City city) {

        int oldLevel = buildingLevel;

        boolean hasElectricity = electricity > 0;
        boolean hasWater       = water       > 0;
        boolean hasInternet    = internet    > 0;

        if (!hasElectricity || !hasWater || !hasInternet) {
            buildingLevel = 0;
            output        = 0;
            demand        = 1;
            System.out.println("House at (" + row + "," + col + ") generated 0 population");
            if (oldLevel > 0) {
                System.out.println("House at (" + row + "," + col + ") levels down from " + oldLevel + " to 0");
            }
            return;
        }

        int targetLevel = 1;
        if (isSecurity && isHealth && isEducation) {
            targetLevel = 2;
            if (currentLifestyle > 0) {
                targetLevel = 3;
            }
        }

        if (targetLevel > buildingLevel) {
            buildingLevel = buildingLevel + 1;
        } else if (targetLevel < buildingLevel) {
            buildingLevel = buildingLevel - 1;
        }

        int min = electricity;
        if (water    < min) min = water;
        if (internet < min) min = internet;

        if (buildingLevel == 0) {
            output = 0;
        } else if (buildingLevel == 1) {
            output = min;
        } else if (buildingLevel == 2) {
            output = 2 * min;
        } else if (buildingLevel == 3) {
            output = 2 * min + currentLifestyle;
        }

        if (output > 1) {
            demand = output;
        } else {
            demand = 1;
        }

        System.out.println("House at (" + row + "," + col + ") generated " + output + " population");

        if (buildingLevel > oldLevel) {
            System.out.println("House at (" + row + "," + col + ") levels up from " + oldLevel + " to " + buildingLevel);
        } else if (buildingLevel < oldLevel) {
            System.out.println("House at (" + row + "," + col + ") levels down from " + oldLevel + " to " + buildingLevel);
        }
    }

    @Override
    public void calculateProduction(City city) {
        city.addToPopulationStorage(output);
    }
}

class Industrial extends Building implements Connectable, Upgrade, ResourceProducer {

    private int currentPopulation;

    public Industrial(int row, int col) {
        super(row, col, 'I', "Industrial");
        this.currentPopulation = 0;
    }

    @Override
    public boolean canConnect() { return true; }

    @Override
    public void resetForNewTick() {
        super.resetForNewTick();
        currentPopulation = 0;
    }

    public void setCurrentPopulation(int amount) {
        this.currentPopulation = amount;
    }

    @Override
    public void updateBuilding(City city) {

        int oldLevel = buildingLevel;

        boolean hasElectricity = electricity > 0;
        boolean hasWater       = water       > 0;

        if (!hasElectricity || !hasWater) {
            buildingLevel = 0;
            output        = 0;
            demand        = 1;
            System.out.println("Industrial at (" + row + "," + col + ") generated 0 goods");
            if (oldLevel > 0) {
                System.out.println("Industrial at (" + row + "," + col + ") levels down from " + oldLevel + " to 0");
            }
            return;
        }

        int targetLevel = 1;
        if (isSecurity) {
            targetLevel = 2;
            if (currentPopulation > 0) {
                targetLevel = 3;
            }
        }

        if (targetLevel > buildingLevel) {
            buildingLevel = buildingLevel + 1;
        } else if (targetLevel < buildingLevel) {
            buildingLevel = buildingLevel - 1;
        }

        int min = electricity;
        if (water < min) min = water;

        if (buildingLevel == 0) {
            output = 0;
        } else if (buildingLevel == 1) {
            output = min;
        } else if (buildingLevel == 2) {
            output = 2 * min;
        } else if (buildingLevel == 3) {
            output = 2 * min + currentPopulation;
        }

        if (output > 1) {
            demand = output;
        } else {
            demand = 1;
        }

        System.out.println("Industrial at (" + row + "," + col + ") generated " + output + " goods");

        if (buildingLevel > oldLevel) {
            System.out.println("Industrial at (" + row + "," + col + ") levels up from " + oldLevel + " to " + buildingLevel);
        } else if (buildingLevel < oldLevel) {
            System.out.println("Industrial at (" + row + "," + col + ") levels down from " + oldLevel + " to " + buildingLevel);
        }
    }

    @Override
    public void calculateProduction(City city) {
        city.addToGoodsStorage(output);
    }
}


class Commercial extends Building implements Connectable, Upgrade, ResourceProducer {

    private int currentPopulation;
    private int currentGoods;

    public Commercial(int row, int col) {
        super(row, col, 'C', "Commercial");
        this.currentPopulation = 0;
        this.currentGoods = 0;
    }

    @Override
    public boolean canConnect() { return true; }

    @Override
    public void resetForNewTick() {
        super.resetForNewTick();
        currentPopulation = 0;
        currentGoods = 0;
    }

    public void setCurrentPopulation(int amount) { this.currentPopulation = amount; }
    public void setCurrentGoods(int amount)      { this.currentGoods = amount; }


    @Override
    public void updateBuilding(City city) {

        int oldLevel = buildingLevel;

        boolean hasElectricity = electricity > 0;
        boolean hasWater       = water       > 0;
        boolean hasInternet    = internet    > 0;

        if (!hasElectricity || !hasWater || !hasInternet) {
            buildingLevel = 0;
            output        = 0;
            demand        = 1;
            System.out.println("Commercial at (" + row + "," + col + ") generated 0 lifestyle");
            if (oldLevel > 0) {
                System.out.println("Commercial at (" + row + "," + col + ") levels down from " + oldLevel + " to 0");
            }
            return;
        }

        int targetLevel = 1;
        if (isSecurity) {
            targetLevel = 2;
            if (currentPopulation > 0 && currentGoods > 0) {
                targetLevel = 3;
            }
        }

        if (targetLevel > buildingLevel) {
            buildingLevel = buildingLevel + 1;
        } else if (targetLevel < buildingLevel) {
            buildingLevel = buildingLevel - 1;
        }

        int min = electricity;
        if (water    < min) min = water;
        if (internet < min) min = internet;

        int min2 = currentPopulation;
        if (currentGoods < min2) min2 = currentGoods;

        if (buildingLevel == 0) {
            output = 0;
        } else if (buildingLevel == 1) {
            output = min;
        } else if (buildingLevel == 2) {
            output = 2 * min;
        } else if (buildingLevel == 3) {
            output = 2 * min + min2;
        }

        if (output > 1) {
            demand = output;
        } else {
            demand = 1;
        }

        System.out.println("Commercial at (" + row + "," + col + ") generated " + output + " lifestyle");

        if (buildingLevel > oldLevel) {
            System.out.println("Commercial at (" + row + "," + col + ") levels up from " + oldLevel + " to " + buildingLevel);
        } else if (buildingLevel < oldLevel) {
            System.out.println("Commercial at (" + row + "," + col + ") levels down from " + oldLevel + " to " + buildingLevel);
        }
    }

    @Override
    public void calculateProduction(City city) {
        city.addToLifestyleStorage(output);
    }
}
class PowerPlant extends Building implements UtilityProvider {

    public static final int capacity = 100;

    public PowerPlant(int row, int col) {
        super(row, col, 'P', "Power Plant");
    }

    @Override
    public void distributeUtilities(City city) {
        city.deliverBFS(row, col, "electricity", capacity);
    }
}

class WaterPlant extends Building implements UtilityProvider {

    public static final int capacity = 100;

    public WaterPlant(int row, int col) {
        super(row, col, 'W', "Water Plant");
    }

    @Override
    public void distributeUtilities(City city) {
        city.deliverBFS(row, col, "water", capacity);
    }
}

class InternetProvider extends Building implements UtilityProvider {

    public static final int capacity = 100;

    public InternetProvider(int row, int col) {
        super(row, col, 'T', "Internet Provider");
    }

    @Override
    public void distributeUtilities(City city) {
        city.deliverBFS(row, col, "internet", capacity);
    }
}


class Empty extends Building {

    public Empty(int row, int col) {
        super(row, col, 'E', "Empty");
    }
}


class Road extends Building implements Connectable {

    public Road(int row, int col) {
        super(row, col, 'R', "Road");
    }

    @Override
    public boolean canConnect() {
        return true;
    }
}
package Level;

import java.awt.Color;
import java.io.FileNotFoundException;

import javax.swing.ImageIcon;

import Bounce.World;
import Bounce.RigidBody;
import Data.Database;
import Entity.CharacterEntity;
import Entity.EntityFactory;
import Sound.MidiPlayer;
import Util.Vector2;

public class LevelFactory 
{
	private EntityFactory factory;
	private Database database;
	private World physics;
	private MidiPlayer midiPlayer;
	private CharacterEntity player;
	private Level level;
	
	public LevelFactory( Database database, World physics, MidiPlayer midiPlayer, EntityFactory factory )
	{
		this.database = database;
		this.physics = physics;
		this.midiPlayer = midiPlayer;	
		this.factory = factory;
	}
	
	public Level buildLevel( String levelID, CharacterEntity player )
	{	
		this.player = player;
		
		//Set level map type
		if( Boolean.parseBoolean( database.get( "levels." + levelID + ".map", "hasMap" )))
		{
			//System.out.print( game.database.get("levels."+ levelID + ".map", "tmxFile"));
			try
			{
				TiledMap tiledMap = new TiledMap( database.get( "levels."+ levelID + ".map", "tmxFile" ));
				
				//TODO: This is stupid, integrate it with entity loading sometime
				if ( player == null ) {
					int numObjects = tiledMap.getObjectCount(0);
			    	for(int i=0; i<numObjects; i++){
			    		
			    		String name = tiledMap.getObjectName( 0, i );
						String[] type = tiledMap.getObjectType( 0, i ).split(":");
						Vector2 pos = new Vector2(tiledMap.getObjectX( 0, i ), tiledMap.getObjectY(0, i));
						System.out.println( type[0] + " " + type[1] + " " + name + " " + pos );
			    		
			    		//Player
			    		if( type[0].equals( "player" ))
			    		{  
			    			try 
			    			{
			    				player = factory.buildPlayer( "ball", (int)pos.x, (int)pos.y );
			    			}
			    			catch (FileNotFoundException e) 
			    			{
			    				// TODO Auto-generated catch block
			    				e.printStackTrace();
			    			}
			    			
			    			this.player = player;
			    		}
			    	}
				}
				
				
				level = new Level( this.player, physics, midiPlayer, levelID );
				
				//Set whether background should be drawn
				level.mDrawBackground = Boolean.parseBoolean( database.get("config", "draw_background" ));
				String colString = database.get("config", "background_color");
				if ( !"".equals(colString)) {
					String[] rgb = (database.get("config", "background_color").split(","));
					level.backgroundColor = new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
				}
				level.addTiledMap(tiledMap);
				buildPhysicalLayer(tiledMap);
				buildEntityLayer(tiledMap);
//				else{
//					
//					this.player = player;
//					
//				}
			}
			catch(Exception ex)
			{
					System.out.print(ex.getMessage());
			}
			finally
			{
			}			
		}
			
		int numOfBackgrounds = Integer.parseInt( database.get( "levels." + levelID + ".backgrounds", "numOfBackgrounds" ));
		for( int i = 0; i < numOfBackgrounds; i++ )
		{
			level.addBackground( buildImageLayer( database.get( "levels." + levelID + ".backgrounds", "background" + i )));
		}	 
		
		int numOfForegrounds = Integer.parseInt( database.get( "levels." + levelID + ".foregrounds", "numOfForegrounds" ));
		for( int i = 0; i < numOfForegrounds; i++ )
		{
			level.addForeground( buildImageLayer( database.get( "levels." + levelID + ".foregrounds", "foreground" + i )));
		}
		
		String midiFile = database.get( "levels."+ levelID, "midiFile" );
		if ( !midiFile.equals(""))
			level.playMusic( midiFile );
		
		return level;
	}
	
	private void buildPhysicalLayer(TiledMap tiledMap) {
		
    	for(int x=0; x<tiledMap.width; x++){
    		for(int y=0; y<tiledMap.height; y++){
    			int tId = tiledMap.getTileId(x, y, 0);//Currently only supports a single layer
    			if(tiledMap.getTileProperty(tId, "isPhysical", "false").equals("true")){
	    			float xPos = tiledMap.tileWidth*x;
	    			float yPos = tiledMap.tileHeight*y;
	    			float height = tiledMap.tileHeight;
	    			float width = tiledMap.tileWidth;
	    			RigidBody obj = null;
	    			if(tiledMap.getTileProperty(tId, "isHazzard", "false").equals("true"))
	    			{	    					
	    				obj = new RigidBody( "groundHazzard", new Vector2(xPos + width / 2, yPos + height / 2), 0);
	    			}
	    			else
	    			{
	    				obj = new RigidBody( "ground", new Vector2(xPos + width / 2, yPos + height / 2), 0);
	    			}	    			
	    			obj.add( new Vector2( xPos, yPos ));
	    			obj.add( new Vector2( xPos + width, yPos ));
	    			obj.add( new Vector2( xPos + width, yPos + height ));
	    			obj.add( new Vector2( xPos, yPos + height ));
	    			obj.setRestitution( .8f );
	    			physics.add( obj );
    			}
    		}
    	}
		
	}
	
	private void buildEntityLayer(TiledMap tiledMap) {
    	int numObjects = tiledMap.getObjectCount(0);
    	for(int i=0; i<numObjects; i++){
    		
    		String name = tiledMap.getObjectName( 0, i );
			String[] type = tiledMap.getObjectType( 0, i ).split(":");
			Vector2 pos = new Vector2(tiledMap.getObjectX( 0, i ), tiledMap.getObjectY(0, i));
			System.out.println( type[0] + " " + type[1] + " " + name + " " + pos );
			
    		
			//Ghost objects
    		if( type[0].equals( "ghost" ))
    		{
    			level.addEntity( factory.buildGhost( name, type[1], pos ));
    		}
    		
    		//Object
    		else if( type[0].equals( "object" ))
    		{
    			level.addEntity( factory.buidGameEntity( name, type[1], pos ));
    		}
    		
    		//Enemies
    		else if( type[0].equals( "enemy" ))
    		{  
    			level.addEntity( factory.BuildEnemy( name, type[1], pos ));    						
    		}
    		
    		//ParticleEmitters
    		else if( type[0].equals( "emitter" ))
    		{
    			level.addEntity( factory.buildEmitter( name, type[1], pos ));
    		}
    	}
	}

	public ImageLayer buildImageLayer( String name )
	{
		ImageLayer layer = new ImageLayer( name );
		
		float x = Float.parseFloat( database.get( "layers.image." + name, "xoffset" ));
		float y = Float.parseFloat( database.get( "layers.image." + name, "yoffset" ));
		layer.setOffset( new Vector2( x, y ));
		
		int numOfImages = Integer.parseInt( database.get( "layers.image." + name, "numOfImages" ));
		for( int i = 0; i < numOfImages; i++ )
		{			
			layer.addImage( new ImageIcon( "content/images/" + database.get( "layers.image." + name, "image" + i )).getImage());
		}	
		
		return layer;
	}
}

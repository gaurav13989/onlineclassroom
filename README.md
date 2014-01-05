********************************************************************************

        				                           Online Classroom
        				                          Gaurav Komera, RIT


********************************************************************************
	
      	Online Classroom was a term project that I implemendted as
      	a part of the Foundations of Computer Networks course at RIT.
      	
      	The main idea behind it is to ease the distribution of video feed
      	from the instructor machine to student systems with the help of
      	P2PTV overlay networks using a Binary Tree. The instructor is the
      	root of the binary tree and has the first two students as its
      	leaves. Any additional students are added to the tree with the
      	previously added students as their parents.
      
      	This however requires the student machines to have a good uplink
      	speed as well, because apart from the instructor the non leaf 
      	students also distribute the video feed. Thus the binary tree.
      
       	It consists of three modules with one java file each.
      	1. Server
      		It should always be on. When an instructor starts, it
      	connects to the server first. The server then assigns the instr-
      	uctor a unique classroom number which he distributes to the
      	students in whichever way(e.g. email). The server maintains a
      	list of all active classes along with the IP address of the 
      	instructor that started the class. The classroom number assigned
      	is a unique random number.
      
      	2. Instructor
      		Instsurtor connects to the server first and receives its
      	assigned classroon number. It accepts video feed from the mach-
      	ne's web camera and unicasts it to two students. It also accepts
      	student requests and assigns them appropriate parents in the tree.
      	New students are added to the tree in a level order fashion. The 
      	parents are also informed about their newly attached children.
      	In case a student drops out because of some reason(e.g. a parent
      	gets dropped and thus all its descendants), the instructor adjusts
      	the tree by replacing the defunct node with the last child
      	in the tree. The instructor identifies such non functional nodes
      	by polling the nodes continuously. In case a node doesn't reply,
      	the instructor tries again twice and in case of a failure read-
      	justs the tree. The dependencies attached in the zip file are all
      	needed for the instructor's java file.
      
      	
      	3. Student
      		Each student needs a classroom number to join the class.
      	The student first connects to the server using the classroom nu-
      	mber and the server on successfully finding the class provides
      	the student with the instructor's IP address. The student now
      	requests the instructor to add it to the class and receives(and
      	forwards) the video feed.
      
      	The presentation can be found here,
      	https://docs.google.com/presentation/d/1sFZ_gfPyTI68pCGThY7tDBRkqlG_zZhxQMA9Mqs657o/edit?usp=sharing


********************************************************************************

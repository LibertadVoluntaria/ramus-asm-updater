package org.exobot.updater;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import org.exobot.game.loader.ClientLoader;
import org.exobot.updater.container.*;
import org.exobot.updater.processor.AddMethodProcessor;
import org.exobot.updater.processor.Processor;
import org.exobot.util.ExoMap;
import org.exobot.util.RIS;
import org.exobot.util.io.StreamWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Ramus
 */
public class Updater implements Runnable {

	private static Updater instance;

	public static Updater getInstance() {
		return instance;
	}

	public static void main(final String[] args) {
		instance = new Updater();
		instance.run();
	}

	private final List<HookContainer> containers = new LinkedList<HookContainer>();
	private final ExoMap<String, ClassNode> classes = new ExoMap<String, ClassNode>();

	@Override
	public void run() {
		System.out.println("[ - Ramus' and Vulcan's ASM Updater - ]");
		System.out.println();
		final ClientLoader loader = new ClientLoader();
		classes.putAll(loader.getClasses());
		loadContainers();
		executeContainers();
		generateModscript();
	}

	private void executeContainers() {
		System.out.println("[ - RuneScape #" + getRSBuild() + " - ]");
		System.out.println();
		System.out.println("Executing containers.");
		System.out.println();
		final long startTime = System.currentTimeMillis();
		Map<String, ClassNode> classes = new LinkedHashMap<String, ClassNode>(this.classes);
		for (final HookContainer hc : containers) {
			for (final Map.Entry<String, ClassNode> entry : classes.entrySet()) {
				final String name = entry.getKey();
				final ClassNode cn = entry.getValue();
				if (!hc.validate(name, cn)) {
					continue;
				}
				for (final Task task : hc.getTasks()) {
					task.run(name, cn);
				}
				classes = new ExoMap<String, ClassNode>(this.classes);
			}
		}
		final double finishTime = (System.currentTimeMillis() - startTime) / 1000.0;
		int totalGetters = 0;
		int finalInterfaces = 0;
		for (final HookContainer hc : containers) {
			if (hc.isHidden()) {
				continue;
			}
			totalGetters += hc.getMethods();
			finalInterfaces += hc.getInterfaces();
		}
		int getters = 0;
		int interfaces = 0;
		Collections.sort(containers, new Comparator<HookContainer>() {

			@Override
			public int compare(final HookContainer hc1, final HookContainer hc2) {
				return hc1.getClass().getSimpleName().compareTo(hc2.getClass().getSimpleName());
			}
		});
		for (final HookContainer hc : containers) {
			if (hc.isHidden()) {
				continue;
			}
			System.out.println("[ - " + hc.getName() + " - ]");
			for (final Processor p : hc.getProcessors()) {
				if (p.getOutput() == null || p.getOutput().isEmpty()) {
					continue;
				}
				switch (p.getId()) {
					case Processor.Id.ADD_METHOD:
					case Processor.Id.GET_STATIC:
					case Processor.Id.GET_FIELD:
						getters++;
						break;
					case Processor.Id.ADD_INTERFACE:
						interfaces++;
						break;
				}
				System.out.println(p.getOutput());
			}
			System.out.println();
		}
		System.out.println("Successfully executed containers in " + finishTime + " seconds.");
		System.out.println();
		System.out.println("Successfully identified " + interfaces + "/" + finalInterfaces + " classes.");
		System.out.println("Successfully identified " + getters + "/" + totalGetters + " fields.");
		System.out.println();
	}

	private void generateModscript() {
		try {
			System.out.println("Generating modscript.");
			final StreamWriter stream = new StreamWriter();
			final List<Processor> processors = new LinkedList<Processor>();
			int numProcessors = 0;
			for (final HookContainer hc : containers) {
				if (hc.isHidden()) {
					continue;
				}
				numProcessors += hc.getProcessors().size();
				processors.addAll(hc.getProcessors());
			}
			Collections.sort(processors, new Comparator<Processor>() {

				@Override
				public int compare(final Processor p, final Processor q) {
					return Integer.compare(p.getId(), q.getId());
				}
			});
			stream.writeInt(0xFADFAD);
			stream.writeString("ramus-exobot-v1.ms");
			stream.writeShort(getRSBuild());
			stream.writeShort(numProcessors);
			stream.write(0xA);
			HookContainer currContainer = null;
			for (final Processor p : processors) {
				if (p instanceof AddMethodProcessor) {
					if (!p.getContainer().equals(currContainer) || currContainer == null) {
						currContainer = p.getContainer();
						stream.write(Processor.Id.GET_FIELD);
						stream.writeString(classes.containsKey(currContainer.getName()) ? classes.get(currContainer.getName()).name : currContainer.getName().toLowerCase());
						stream.writeShort(currContainer.getSize(AddMethodProcessor.class));
					}
				}
				p.process(stream);
			}
			stream.subSequence(0, stream.size() - 1);
			stream.writeTo(new FileOutputStream(new File("modscript.dat")));
			System.out.print("Successfully generated modscript.");
		} catch (final Exception e) {
			System.out.print("Unable generate modscript.");
		}
	}

	public ExoMap<String, ClassNode> getClasses() {
		return classes;
	}

	private int getRSBuild() {
		final ClassNode client = classes.get("client");
		for (final MethodNode mn : client.methods) {
			if (!mn.name.equals("init") || !mn.desc.equals("()V")) {
				continue;
			}
			final RIS ris = new RIS(mn);
			IntInsnNode sipush;
			ris.setPosition(ris.getInsnList().size() - 1);
			while ((sipush = ris.previous(IntInsnNode.class, Opcodes.SIPUSH)) != null) {
				if (sipush.operand > 256) {
					return sipush.operand;
				}
			}
		}
		return -1;
	}

	private void loadContainers() {
		System.out.println("Loading containers.");
		containers.add(new DataContainer());
		containers.add(new AnimableNodeContainer());
		containers.add(new ClientContainer());
		containers.add(new InteractablePlaneContainer());
		containers.add(new ModelContainer());
		containers.add(new NodeContainer());
		containers.add(new NodeSubContainer());
		containers.add(new NpcNodeContainer());
		containers.add(new RenderContainer());
		containers.add(new TileDataContainer());
		System.out.println("Successfully loaded " + containers.size() + " containers.");
		System.out.println();
	}
}
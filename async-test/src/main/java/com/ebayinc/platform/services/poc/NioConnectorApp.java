package com.ebayinc.platform.services.poc;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.asynchttpclient.DefaultAsyncHttpClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.rx.RxClient;
import org.glassfish.jersey.client.rx.rxjava.RxObservable;
import org.glassfish.jersey.client.rx.rxjava.RxObservableInvoker;
import org.glassfish.jersey.media.sse.EventListener;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import com.ebayinc.platform.jaxrs.ahc.connector.AsyncHttpClientConnectorProvider;

public class NioConnectorApp {
	
	private static final String URL = "http://www.json-generator.com/api/json/get/cvJIUtuoOa?indent=2";
	private static final String URL2 = "http://www.json-generator.com/api/json/get/cvaKzUtMRK?indent=2";
	private static final String URL3 = "http://52.11.54.32:8080/philae/services/v1/events";
	
	private static Client sClient = null;
	private static boolean print = true;
	private static int times = 20;
	public static void main(String[] args) {
		warmup();
		asyncInvocation();
		asyncInvocationWithCallback();
		//sseInvocation();
		/*rxInvocation();*/
		asyncInvocationWOAhc();
	}
	
	private static void warmup() {
		print = false;
		for (int i=0;i<times;i++) {
			asyncInvocation();
			asyncInvocationWithCallback();
			asyncInvocationWOAhc();
		}
		print = true;
		System.out.println("Warming up done...");
	}

	private static void asyncInvocationWOAhc() {
		Client client = ClientBuilder.newClient();
		Future<Response> responseFuture = client.target(URL).request().async().get();
		long start = System.currentTimeMillis();
		while (!responseFuture.isDone());
		long span = System.currentTimeMillis() - start;
		
		try {
			print(responseFuture.get(), span, "Async w/o AHC," + Thread.currentThread().getName());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	private static void asyncInvocation(){
		Client client = configure();
		Future<Response> responseFuture = client.target(URL).request().async().get();
		long start = System.currentTimeMillis();
		while (!responseFuture.isDone());
		long span = System.currentTimeMillis() - start;
		
		try {
			print(responseFuture.get(), span, "Async," + Thread.currentThread().getName());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
	
	private static void asyncInvocationWithCallback(){
		Client client = configure();
		final long start = System.currentTimeMillis();
		client.target(URL).request().async().get(new InvocationCallback<Response>() {

			public void completed(Response response) {
				print(response, System.currentTimeMillis() - start, "AsyncWithCallback," + Thread.currentThread().getName());
			}

			public void failed(Throwable throwable) {
				throwable.printStackTrace();
			}
		});
	}
	
	private static void rxInvocation() {
		Client client = configure();
		RxClient<RxObservableInvoker> rxClient = RxObservable.from(client);
		Observable<List<Employee>> observable = rxClient.target(URL2).request().rx().get(new GenericType<List<Employee>>(){});
		observable.flatMap(new Func1<List<Employee>, Observable<? extends Employee>>() {
			public Observable<? extends Employee> call(List<Employee> employees) {
				return Observable.from(employees);
			}
		})
		.map(new Func1<Employee, Long>() {
			public Long call(Employee employee) {
				return employee.getSalary();
			}
		})
		.reduce(new Func2<Long, Long, Long>() {
			public Long call(Long salary1, Long salary2) {
				return salary1 + salary2;
			}
		}).subscribe(new Action1<Long>() {
			public void call(Long totalSalary) {
				System.out.println("Total salary of employees: " + totalSalary);
			}
		});
	}

	private static void sseInvocation() {
		final long sleep = 20000;
		Client client = ClientBuilder.newClient().register(SseFeature.class);
		WebTarget target = client.target(URL3);
		EventSource eventSource = EventSource.target(target).build();
		
		EventListener eventListener = new EventListener() {
			
			public void onEvent(InboundEvent inboundEvent) {
				System.out.println("threadId: "+ Thread.currentThread().getName() +"\nevent: " + inboundEvent.getName() + "\ndata: " + inboundEvent.readData(String.class));
			}
		};
		eventSource.register(eventListener);
		eventSource.open();
		
		try {
			Thread.sleep(sleep);//wait for 20 seconds
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
		
		eventSource.close();
	}
	
	private static void print(Response response, long span, String qualifier){
		if (!print) {
			return;
		}
		//System.out.println("Out: " + response.readEntity(String.class) + "\n");
		System.out.println("<ResponseTime,MethodCall,ThreadId> := <" + span + " ms," + qualifier + ">");
	}
	
	private static Client configure(){
		if (sClient != null)
			return sClient;
		
		ClientConfig config = new ClientConfig();
		config.connectorProvider(new AsyncHttpClientConnectorProvider(new DefaultAsyncHttpClient()));
		sClient = ClientBuilder.newClient(config);
		return sClient;
	}
	
	public static class Employee {
		private String _id;
		private String name;
		private String gender;
		private String company;
		private String email;
		private int age;
		private long salary;
		public String get_id() {
			return _id;
		}
		public void set_id(String _id) {
			this._id = _id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getGender() {
			return gender;
		}
		public void setGender(String gender) {
			this.gender = gender;
		}
		public String getCompany() {
			return company;
		}
		public void setCompany(String company) {
			this.company = company;
		}
		public String getEmail() {
			return email;
		}
		public void setEmail(String email) {
			this.email = email;
		}
		public int getAge() {
			return age;
		}
		public void setAge(int age) {
			this.age = age;
		}
		public long getSalary() {
			return salary;
		}
		public void setSalary(long salary) {
			this.salary = salary;
		}
	}
}

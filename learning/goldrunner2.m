close all;
clear all;

nhidden = 50; %number of neurons on the hidden layer
load('datos.mat');
tmpXtrain = Xtrain; tmpXCVal = XCVal; 
[nt,m] = size(Xtrain);
nv = size(XCVal,1);
mut = mean(Xtrain);
devt = std(Xtrain);
Xtrain = (tmpXtrain - repmat(mut,nt,1)) ./ (repmat(devt,nt,1));
XCVal = (tmpXCVal - repmat(mut,nv,1)) ./ (repmat(devt,nv,1));

nn = createnn(m,nhidden,1);
compnn = @(nn1,X1)(regnn(nn1,X1));
errnn = @(nn1,X1,R1)(regerrnn(nn1,X1,R1));

wv=[nn.W(:);nn.V(:)];
nn = nntrain(nn,Xtrain,Ytrain,150,compnn,errnn);
yp = regnn(nn,Xtrain);
cmp = [yp Ytrain];
cmp(1:10,:)

terr = sqrt(errnn(nn,Xtrain, Ytrain)/nt);
verr = sqrt(errnn(nn,XCVal, YCVal)/nv);
printf("Training Error: %f, Val. error: %f\n",terr,verr);
V = nn.V;
W = nn.W;
save("-binary","learned.mat","mut","devt","V","W");
